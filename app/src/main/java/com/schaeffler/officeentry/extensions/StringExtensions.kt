package com.schaeffler.officeentry.extensions

import android.graphics.Bitmap
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.BarcodeEncoder

/**
 * Parse JSON [String] into the data class.
 *
 * @param T The data class type.
 * @return The parsed data class.
 */
inline fun <reified T> String.parseJson(): T = Gson().fromJson(this, T::class.java)

/**
 * Converts the [String] content into a QR code [Bitmap].
 *
 * @param width The width of the [Bitmap].
 * @param height The height of the [Bitmap].
 *
 * @return The QR code image.
 */
fun String.toQrCode(width: Int, height: Int): Bitmap =
    MultiFormatWriter().encode(
        this,
        BarcodeFormat.QR_CODE,
        (width * 1.5).toInt(),
        (height * 1.5).toInt()
    ).run {
        BarcodeEncoder().createBitmap(this)
    }