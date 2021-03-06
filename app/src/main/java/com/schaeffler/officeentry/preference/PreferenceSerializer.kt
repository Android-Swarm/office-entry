package com.schaeffler.officeentry.preference

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import com.schaeffler.officeentry.Preference
import java.io.InputStream
import java.io.OutputStream

object PreferenceSerializer : Serializer<Preference> {
    override fun readFrom(input: InputStream): Preference = try {
        Preference.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
        throw CorruptionException("Cannot read proto.", e)
    }

    override fun writeTo(t: Preference, output: OutputStream) = t.writeTo(output)

    override val defaultValue: Preference
        get() = Preference.getDefaultInstance()

}