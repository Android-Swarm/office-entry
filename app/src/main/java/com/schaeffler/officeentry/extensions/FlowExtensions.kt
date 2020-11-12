package com.schaeffler.officeentry.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 * Executes the given [block] without transforming the [Flow]. Internally, each value will
 * get an [also] call.
 *
 * @param block The additional action to do.
 */
fun <T> Flow<T>.alsoDo(block: suspend (T) -> Unit) = map { it.also { block(it) } }