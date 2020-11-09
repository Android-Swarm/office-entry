package com.schaeffler.officeentry.ui

import android.content.Context
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import com.google.android.material.snackbar.Snackbar
import com.robotemi.sdk.TtsRequest
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.extensions.alsoDo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
class MainActivityViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context
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
    }

    fun showSnackBar(stringId: Int, length: Int = Snackbar.LENGTH_LONG) =
        _snackBarFlow emitValue (getString(stringId) to length)

    fun requestTemiSpeak(stringId: Int, show: Boolean = false) =
        _ttsRequestFlow emitValue (getString(stringId) to show)

    fun setMaskDetected(detected: Boolean) {
        _maskDetected.value = detected
    }

    private fun getString(id: Int) = context.getString(id)
}