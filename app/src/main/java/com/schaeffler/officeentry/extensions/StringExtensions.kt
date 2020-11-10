package com.schaeffler.officeentry.extensions

import com.google.gson.Gson

inline fun <reified T> String.parseJson(): T = Gson().fromJson(this, T::class.java)