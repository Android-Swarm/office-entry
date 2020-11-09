package com.schaeffler.officeentry.extensions

import android.content.Context

/** Tag for logging. */
val Context.TAG
    get() = this::class.simpleName