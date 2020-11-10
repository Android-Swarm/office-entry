package com.schaeffler.officeentry.extensions

import java.io.InputStream

/**
 * Reads up to [length] bytes from the [InputStream].
 *
 * @param length The maximum length to read.
 * @return The data obtained from the [InputStream].
 */
infix fun InputStream.readBytesLength(length: Int): ByteArray {
    return ByteArray(length).apply {
        read(this, 0, length)
    }
}

/**
 * Reads up to [length] bytes from the [InputStream], and decode it to a [String].
 *
 * @param length The maximum length to read.
 * @return The data obtained from the [InputStream].
 */
infix fun InputStream.readStringLength(length: Int): String {
    return readBytesLength(length).decodeToString()
}