package com.schaeffler.officeentry.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import com.ogawa.temiirsdk.IrManager
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.databinding.ActivityMainBinding
import com.schaeffler.officeentry.extensions.TAG
import com.schaeffler.officeentry.extensions.collectLatestStream
import com.schaeffler.officeentry.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startMaskDetection()
            } else {
                viewModel.showSnackBar(R.string.snack_bar_camera_rejected)
            }
        }

    private lateinit var binding: ActivityMainBinding

    /** For detecting mask. */
    @Inject
    lateinit var maskDetector: MaskDetector

    /** For detecting face. */
    @Inject
    lateinit var faceDetector: FaceDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set to night mode
        if (!isNightMode) {
            switchNightMode(true)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        // Request cam for mask detection
        requestPermission.launch(CAMERA_PERMISSION)

        // Show snack bar
        lifecycleScope.collectLatestStream(viewModel.snackBarFlow) { (message, length) ->
            Snackbar.make(binding.root, message, length)
                .setAction(android.R.string.ok) { /* Dismiss snack bar on click */ }
                .show()
        }

        // Speak tts
        lifecycleScope.collectLatestStream(viewModel.ttsRequestFlow, robot::speak)

        // Logic for each application states
        lifecycleScope.collectLatestStream(viewModel.appState) { state ->
            when (state) {
                AppState.IDLE -> {
                    viewModel.userInteraction.filter { it }
                        .take(1)
                        .collect {
                            Log.d(TAG, "Face detected, start temperature collection")
                            viewModel.updateApplicationState(AppState.COLLECTING)
                        }
                }

                AppState.COLLECTING -> {
                    val totalTemp = viewModel.temperatureFlow.take(TEMP_COUNT)
                        .onEach { viewModel.updateReceivedFirstTemperature(true) }
                        .onCompletion {
                            Log.d(TAG, "Received $TEMP_COUNT valid temperatures")
                            viewModel.updateReceivedFirstTemperature(false)
                        }
                        .reduce { acc, value -> acc + value }

                    val finalTemp = totalTemp / TEMP_COUNT

                    Log.d(TAG, "Final temperature: %.2f".format(finalTemp))
                    viewModel.finalizeTemperature(finalTemp)
                    viewModel.updateApplicationState(AppState.COMPLETE)
                }

                AppState.COMPLETE -> {
                    (1..7).asFlow().onEach { delay(1000) }
                        .onCompletion {
                            Log.d(TAG, "End of delay, returning to ${AppState.IDLE}")
                            viewModel.updateApplicationState(AppState.IDLE)
                        }
                        .collect()
                }
            }

            // Inform user that the camera detected temperature
            lifecycleScope.collectLatestStream(viewModel.tempDetecting) { (first, state) ->
                if (first && state == AppState.COLLECTING) {
                    Log.d(TAG, "Received first temperature of the user")
                    viewModel.requestTemiSpeak(R.string.tts_start_collecting)
                }
            }

            // Handle user gone before finalizing temperature
            lifecycleScope.collectLatestStream(
                viewModel.userInteraction
                    .debounce(2000)
            ) { interacting ->
                if (!interacting) {
                    if (viewModel.appState.value == AppState.COLLECTING) {
                        viewModel.updateApplicationState(AppState.IDLE)
                    }
                }
            }
        }

        prepareTemperatureMeasurement()
    }

    private fun startMaskDetection() {
        cameraView.setLifecycleOwner(this)

        cameraView.addFrameProcessor { frame ->
            if (frame.dataClass != ByteArray::class.java) return@addFrameProcessor

            runBlocking {
                val rotation = frame.rotationToUser

                val inputImage = InputImage.fromByteArray(
                    frame.getData(),
                    frame.size.width,
                    frame.size.height,
                    rotation,
                    InputImage.IMAGE_FORMAT_NV21
                )

                val face = faceDetector.process(inputImage).await().firstOrNull()

                viewModel.updateUserInteraction(face != null)

                // Check for mask only if there is a face detected
                face?.let { _ ->
                    maskDetector.detectMask(inputImage).run {
                        viewModel.setMaskDetected(isWearingMask)
                    }
                } ?: viewModel.setMaskDetected(true)

            }
        }

        Log.d(TAG, "Mask detection has been configured.")
    }

    /**
     * Initializes the Temi-IR SDK.
     *
     */
    private fun prepareTemperatureMeasurement() {
        // Temp SDK
        lifecycleScope.launch {
            do {
                IrManager.initIr(
                    application,
                    getString(R.string.temi_ir_key),
                    getString(R.string.temi_ir_app_id),
                )

                delay(1000)

                IrManager.getCheckSuccess().also {
                    Log.d(
                        TAG,
                        if (!it) "Init failed, retrying" else "Init success! Starting measurement"
                    )
                }
            } while (!IrManager.getCheckSuccess())

            viewModel.setTemiIrSdkState(true)
        }
    }

    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA

        /** Number of temperature measurement needed to finalize. */
        const val TEMP_COUNT = 3
    }
}