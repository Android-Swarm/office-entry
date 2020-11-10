package com.schaeffler.officeentry.ui

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.schaeffler.officeentry.extensions.readStringLength
import com.schaeffler.officeentry.extensions.writeString
import com.schaeffler.officeentry.thermal.CameraConfig
import com.schaeffler.officeentry.thermal.withSocketOperation
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WifiInputFragmentViewModel @ViewModelInject constructor() : BaseViewModel() {

    private val ssidFlow = MutableSharedFlow<String>()
    private val passwordFlow = MutableSharedFlow<String>()
    private val credential = ssidFlow.zip(passwordFlow) { ssid, pass -> Pair(ssid, pass) }
        .filter { (ssid, _) -> ssid.isNotBlank() }

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting = _isConnecting.asLiveData()

    private val _macAddress = MutableStateFlow("")
    val macAddress = _macAddress.asLiveData()

    private val _ssidError = MutableSharedFlow<String>()
    val ssidError = _ssidError.asLiveData()

    private val _socketError = MutableSharedFlow<String>()
    val socketError = _socketError.asLiveData()

    init {
        viewModelScope.launch {
            credential.collect { (ssid, pass) ->
                try {
                    resetWlanCamera(ssid, pass)
                } catch (e: Exception) {
                    Log.e(TAG, "Encountered exception on first socket: ", e)
                }
            }
        }
    }

    fun submitCredentials(ssid: String, password: String) {
        ssidFlow emitValue ssid
        passwordFlow emitValue password
    }

    fun submitSsidError(error: String) = _ssidError emitValue error

    private suspend fun resetWlanCamera(ssid: String, password: String) =
        viewModelScope.launch(Dispatchers.IO) {
            _isConnecting.value = true

            try {
                withSocketOperation(
                    CameraConfig.FIRST_SOCKET_IP,
                    CameraConfig.FIRST_SOCKET_PORT
                ) { _, writer, reader ->
                    writer.writeString(CameraConfig.createConnectCommand(ssid, password))
                    Log.d(TAG, "Sent SETWLAN command")

                    val macResponse = reader readStringLength "+OK=AA:BB:CC:DD:EE:FF".length
                    Log.d(TAG, "Response: $macResponse")

                    writer.writeString(CameraConfig.RESET_WLAN_COMMAND)
                    Log.d(TAG, "Sent RSTWLAN command")

                    delay(2000)

                    _macAddress.value = macResponse.drop(4)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Encountered exception on first socket: ", e)
                _socketError emitValue (e.localizedMessage ?: "")
            } finally {
                _isConnecting.value = false
            }
        }
}