package com.schaeffler.officeentry.extensions

import java.io.OutputStream

/**
 * Writes the [String] to the [OutputStream] after decoding it using UTF-8.
 *
 * @param message The [String] to write.
 */
infix fun OutputStream.writeString(message: String) = write(message.toByteArray())