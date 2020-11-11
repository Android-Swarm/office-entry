package com.schaeffler.officeentry.extensions

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter


/**
 * Sets the color filter for this [ImageView].
 *
 * @param color The color.
 */
@BindingAdapter("tintInt")
fun ImageView.tintColor(color: Int?) = color?.let { setColorFilter(color) } ?: clearColorFilter()

@BindingAdapter("srcBitmap")
fun ImageView.bitmapSrc(bitmap: Bitmap?) {
    setImageBitmap(bitmap)
}