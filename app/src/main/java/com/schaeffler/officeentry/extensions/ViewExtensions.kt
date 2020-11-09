package com.schaeffler.officeentry.extensions

import android.animation.ObjectAnimator
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter

/**
 * If [enabled] is `true`, the [View] will play blinking animation. By default, it will blink with
 * the frequency of 2 seconds.
 *
 * @param repeatDuration The half duration of the animation (from invisible to visible or vice versa).
 * @param enabled `true` to play the animation or `false` to stop the animation.
 */
@BindingAdapter(value = ["blinkDuration", "blinkEnabled"], requireAll = false)
fun View.blink(repeatDuration: Long?, enabled: Boolean?) {
    if (enabled != false) {
        tag ?: let {
            ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
                duration = repeatDuration ?: 1000L
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
            }.also {
                tag = it
            }.start()
        }
    } else {
        (tag as? ObjectAnimator)?.let {
            it.end()
            alpha = 1.0f
            tag = null
        }
    }
}

/**
 * Sets the [isVisible] property of the [View].
 *
 * @param isVisible The new value for the [isVisible] property.
 */
@BindingAdapter("isVisible")
fun View.dynamicVisibility(isVisible: Boolean) {
    this.isVisible = isVisible
}

/**
 * Sets the visibility of the [View]. Using this adapter, the [View] will go to [View.INVISIBLE]
 * instead of [View.GONE].
 *
 * @param isVisible The new visibility.
 */
@BindingAdapter("canVisible")
fun View.dynamicInvisibleVisibility(isVisible: Boolean) {
    visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
}