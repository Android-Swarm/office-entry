package com.schaeffler.officeentry.extensions

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Collects the from the [Flow] when the lifecycle is at least in [Lifecycle.State.CREATED] state.
 *
 * @param flow The flow to collect. The operator used is [Flow.collectLatest].
 * @param action The action to do on collection.
 */
inline fun <T> LifecycleCoroutineScope.collectLatestStream(
    flow: Flow<T>,
    crossinline action: suspend (T) -> Unit
) {
    launchWhenCreated {
        flow.collectLatest {
            action(it)
        }
    }
}