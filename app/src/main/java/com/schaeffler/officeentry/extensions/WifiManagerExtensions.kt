package com.schaeffler.officeentry.extensions

import android.net.wifi.WifiManager

/**
 * Returns the WiFi SSID the current device is connected to, after removing the leading and trailing
 * apostrophes.
 */
val WifiManager.currentSsid
    get() = connectionInfo.ssid.drop(1).dropLast(1)