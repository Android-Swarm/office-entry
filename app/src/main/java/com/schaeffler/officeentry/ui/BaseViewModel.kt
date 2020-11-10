package com.schaeffler.officeentry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {
    /**
     * Sends value to the [MutableSharedFlow] asynchronously. This function will create a new child
     * coroutine.
     *
     * @param value The value to send to the flow.
     */
    protected infix fun <T> MutableSharedFlow<T>.emitValue(value: T) = viewModelScope.launch {
        this@emitValue.emit(value)
    }

    /** Tag for logging. */
    @Suppress("PropertyName")
    protected val TAG = this::class.simpleName
}