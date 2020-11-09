package com.schaeffler.officeentry.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

open class BaseViewModel : ViewModel() {
    protected infix fun <T> MutableSharedFlow<T>.emitValue(value: T) = viewModelScope.launch {
        this@emitValue.emit(value)
    }
}