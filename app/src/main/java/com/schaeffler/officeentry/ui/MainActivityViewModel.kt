package com.schaeffler.officeentry.ui

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

class MainActivityViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context
) : BaseViewModel() {
    private val _snackBarFlow = MutableSharedFlow<Pair<String, Int>>()
    val snackBarFlow: SharedFlow<Pair<String, Int>> = _snackBarFlow

    fun showSnackBar(stringId: Int, length: Int = Snackbar.LENGTH_LONG) =
        _snackBarFlow emitValue Pair(context.getString(stringId), length)
}