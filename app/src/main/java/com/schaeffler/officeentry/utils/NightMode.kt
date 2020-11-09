package com.schaeffler.officeentry.utils

import androidx.appcompat.app.AppCompatDelegate

/** Whether the application is in night mode or not. */
val isNightMode: Boolean
    get() = AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_YES

/**
 * Turns night mode on or off.
 *
 * @param useNightMode `true` for on, otherwise off.
 */
fun switchNightMode(useNightMode: Boolean) {
    AppCompatDelegate.setDefaultNightMode(
        if (useNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    )
}