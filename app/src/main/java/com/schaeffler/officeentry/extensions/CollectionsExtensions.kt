package com.schaeffler.officeentry.extensions

import android.graphics.Bitmap
import java.util.*

/**
 * Converts an Array of colors represented in integers to a [Bitmap].
 *
 * @param width The image width.
 * @param height The image height.
 *
 * @return The image.
 */
fun Array<Int>.toBitmap(width: Int, height: Int): Bitmap =
    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        var index = 0

        (0 until height).forEach { y ->
            (0 until width).forEach { x ->
                val color = this@toBitmap[index++]

                setPixel(x, y, color)
            }
        }
    }

/**
 * Converts [ByteArray] to its hexadecimal [String] representation.
 *
 * @param uppercase `true` if the result should be in uppercase.
 */
fun ByteArray.toHexString(uppercase: Boolean = true) =
    joinToString("") { "%02x".format(it) }
        .run {
            if (uppercase) toUpperCase(Locale.ROOT)
            else this
        }