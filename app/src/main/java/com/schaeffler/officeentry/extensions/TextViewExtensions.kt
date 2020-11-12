package com.schaeffler.officeentry.extensions

import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.schaeffler.officeentry.R
import com.schaeffler.officeentry.utils.AppState

val TextView.textString
    get() = text?.toString() ?: ""

/**
 * Sets the text to [noBlankText] if it is not blank. Otherwise, set the text to [noBlankDefault].
 *
 * @param noBlankText The text that will be set if this is not blank.
 * @param noBlankDefault The text that will be set when [noBlankText] is blank or null.
 */
@BindingAdapter("noBlankText", "noBlankDefault")
fun TextView.blankDefault(noBlankText: String?, noBlankDefault: String) {
    text = if (noBlankText == null || noBlankText.isBlank()) {
        noBlankDefault
    } else {
        noBlankText
    }
}

/**
 * Sets the text to [mac] in mac address format if it is not blank. Otherwise, set the text to [noBlankDefault].
 *
 * @param mac The text that will be set if this is not blank.
 * @param noBlankDefault The text that will be set when [mac] is blank or null.
 */
@BindingAdapter("noBlankMac", "noBlankDefault")
fun TextView.blankDefaultMac(mac: String?, noBlankDefault: String) {
    text = if (mac == null || mac.isBlank()) {
        noBlankDefault
    } else {
        mac.chunked(2).joinToString(":")
    }
}

@BindingAdapter("simpleAdapterContent")
fun <T> MaterialAutoCompleteTextView.simpleAdapter(content: List<T>?) {
    setAdapter(ArrayAdapter(context, android.R.layout.simple_list_item_1, content ?: listOf()))
}

@BindingAdapter("celciusTempText")
fun TextView.celciusTemp(temp: Float?) = temp?.let { text = String.format("%.1f℃", temp) }

@BindingAdapter("textState")
fun TextView.appStateText(state: AppState?) {
    text = when (state) {
        null, AppState.IDLE -> context.getString(R.string.label_idle)
        AppState.COLLECTING -> context.getString(R.string.label_collecting)
        AppState.COMPLETE -> context.getString(R.string.label_complete)
    }
}