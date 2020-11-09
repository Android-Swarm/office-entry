package com.schaeffler.officeentry.extensions

import androidx.databinding.ViewDataBinding

/**
 * Automatically calls [ViewDataBinding.executePendingBindings] at the end of the lambda execution.
 *
 * @param T Any subclass of [ViewDataBinding].
 * @param block Use the lambda function to update or assign data to the layout.
 */
inline fun <T : ViewDataBinding> T.executePendingBindings(block: T.() -> Unit) {
    block()
    executePendingBindings()
}