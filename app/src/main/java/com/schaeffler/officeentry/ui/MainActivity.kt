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
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.databinding.ActivityMainBinding
import com.schaeffler.officeentry.extensions.TAG
import com.schaeffler.officeentry.extensions.collectLatestStream
import com.schaeffler.officeentry.utils.MaskDetector
import com.schaeffler.officeentry.utils.isNightMode
import com.schaeffler.officeentry.utils.robot
import com.schaeffler.officeentry.utils.switchNightMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
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

        if (!isNightMode) {
            switchNightMode(true)
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this

        requestPermission.launch(CAMERA_PERMISSION)

        lifecycleScope.collectLatestStream(viewModel.snackBarFlow) { (message, length) ->
            Snackbar.make(binding.root, message, length)
                .setAction(android.R.string.ok) { /* Dismiss snack bar on click */ }
                .show()
        }

        lifecycleScope.collectLatestStream(viewModel.ttsRequestFlow, robot::speak)
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

                val faces = faceDetector.process(inputImage).await()

                // Check for mask only if there is a face detected
                faces.firstOrNull()
                    ?.let { _ ->
                        maskDetector.detectMask(inputImage).run {
                            viewModel.setMaskDetected(isWearingMask)
                        }
                    } ?: viewModel.setMaskDetected(true).also {
                    Log.d(TAG, "No face detected")
                }

            }
        }

        Log.d(TAG, "Mask detection has been configured.")
    }

    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}