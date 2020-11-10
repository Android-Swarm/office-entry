package com.schaeffler.officeentry.extensions

import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.TextInputLayout

/**
 * Sets an error message to the layout.
 *
 * @param error The error message to be displayed.
 */
@BindingAdapter("errorText")
fun TextInputLayout.errorText(error: String?) {
    error?.let { if (it.isNotEmpty()) setError(it) else setError(null) } ?: setError(null)
}

@BindingAdapter("cancelErrorOnEdit")
fun TextInputLayout.cancelErrorOnEdit(cancel: Boolean) {
    if (cancel) {
        editText?.let {
            it.setOnClickListener { error = null }
        }
    }
}