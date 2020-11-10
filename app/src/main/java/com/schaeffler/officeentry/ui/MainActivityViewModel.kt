package com.schaeffler.officeentry.ui

import android.content.Context
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.robotemi.sdk.TtsRequest
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.extensions.alsoDo
import com.schaeffler.officeentry.preference.PreferenceRepository
import com.schaeffler.officeentry.thermal.CameraDetails
import com.schaeffler.officeentry.thermal.CameraDetailsFetcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class MainActivityViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
    private val repository: PreferenceRepository,
) : BaseViewModel() {
    private val _snackBarFlow = MutableSharedFlow<Pair<String, Int>>()
    val snackBarFlow: SharedFlow<Pair<String, Int>> = _snackBarFlow

    private val _ttsRequestFlow = MutableSharedFlow<Pair<String, Boolean>>()
    val ttsRequestFlow = _ttsRequestFlow.map { (message, show) ->
        TtsRequest.create(message, show)
    }

    private val _maskDetected = MutableStateFlow(true)
    val maskDetected = _maskDetected.debounce(1000).alsoDo {
        if (!it) {
            Log.d(TAG, "User not wearing mask!")
            requestTemiSpeak(R.string.tts_no_mask_detected)
        } else {
            Log.d(TAG, "User is wearing mask")
        }
    }.asLiveData()

    private val _cameraDetailsState = MutableStateFlow<CameraDetails?>(null)
    val cameraDetails = _cameraDetailsState.asLiveData()

    private val _batteryString = MutableStateFlow("")
    val batteryString = _batteryString.asLiveData()

    private val _temiIrSdk = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _cameraDetailsState emitValue CameraDetailsFetcher("520291E24E53")
                .cameraDetailsAsync(this)
                .await()
        }
    }

    /** Whether the thermal camera socket is connected and the SDK is ready. */
    val isThermalCameraReady = _temiIrSdk.asLiveData() //TODO: combine with socket connection

    fun showSnackBar(stringId: Int, length: Int = Snackbar.LENGTH_LONG) =
        _snackBarFlow emitValue (getString(stringId) to length)

    fun showSnackBar(stringId: Int, vararg args: Any?, length: Int = Snackbar.LENGTH_LONG) =
        _snackBarFlow emitValue (getString(stringId).format(*args) to length)

    fun requestTemiSpeak(stringId: Int, show: Boolean = false) =
        _ttsRequestFlow emitValue (getString(stringId) to show)

    fun setMaskDetected(detected: Boolean) {
        _maskDetected.value = detected
    }

    fun setTemiIrSdkState(ready: Boolean) {
        _temiIrSdk.value = ready
    }

    fun saveCameraNetworkInfo(mac: String, ip: String) = viewModelScope.launch {
        repository.saveCameraConnectionDetails(mac, ip)
    }

    private fun getString(id: Int) = context.getString(id)
}