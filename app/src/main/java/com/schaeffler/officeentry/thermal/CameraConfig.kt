package com.schaeffler.officeentry.thermal

object CameraConfig {
    const val FIRST_SOCKET_IP = "10.10.100.1"
    const val FIRST_SOCKET_PORT = 5000
    const val RESET_WLAN_COMMAND = "AT+RSTWLAN"

    const val OGAWA_HOTSPOT_NAME = "OGAWA-IR"
    const val BATTERY_SOCKET = 5002
    const val BATTERY_COMMAND = "AT+GETBATTVOLT"
    const val MAX_BATTERY_RESPONSE_LENGTH = "+OK=1000".length

    const val TEMP_PORT = 5001
    const val EEM_COMMAND = "\$SETP=17,4"

    const val INTERPOLATION = 1 // Primary interpolation
    const val IMAGE_WIDTH = 128
    const val IMAGE_HEIGHT = 96

    private fun String.escapeCharForCamera() = replace(",", """\,""")
        .replace("""\""", """\\""")

    fun createConnectCommand(ssid: String, password: String) =
        "AT+SETWLAN=${ssid.escapeCharForCamera()},${password.escapeCharForCamera()}"
}