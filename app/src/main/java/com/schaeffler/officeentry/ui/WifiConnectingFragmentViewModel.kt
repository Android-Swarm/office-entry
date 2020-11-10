package com.schaeffler.officeentry.ui

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.schaeffler.officeentry.preference.PreferenceRepository
import com.schaeffler.officeentry.thermal.CameraDetails
import com.schaeffler.officeentry.thermal.CameraDetailsFetcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class WifiConnectingFragmentViewModel @ViewModelInject constructor() : BaseViewModel() {

    var retryCount = 0
        get() = ++field
        private set

    private val _success = MutableLiveData<Boolean?>(null)
    val success: LiveData<Boolean?> = _success

    private val _cameraDetails = MutableSharedFlow<CameraDetails?>()
    val cameraDetails = _cameraDetails.asLiveData()

    /**
     * Calls the Temi-IR API for the camera details.
     *
     * @param cameraMac The raw mac address.
     */
    fun requestCameraDetails(cameraMac: String) {
        viewModelScope.launch {
            val details = CameraDetailsFetcher(cameraMac)
                .cameraDetailsAsync(this)
                .await()

            _cameraDetails.emit(details)
        }
    }

    /**
     * Updates the state of the screen.
     *
     * @param success `true` if the details has been fetched, otherwise `false`.
     */
    fun updateState(success: Boolean) = _success.postValue(success)
}