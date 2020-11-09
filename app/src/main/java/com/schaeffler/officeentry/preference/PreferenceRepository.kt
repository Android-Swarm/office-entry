package com.schaeffler.officeentry.preference

import android.content.Context
import android.util.Log
import androidx.datastore.createDataStore
import com.schaeffler.officeentry.Preference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for this application's preferences.
 *
 * @param context The application context.
 */
@Singleton
class PreferenceRepository @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.createDataStore(FILE_NAME, PreferenceSerializer)

    /** Contains this application's preferences. */
    private val preference = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preference.", exception)
                emit(Preference.getDefaultInstance())
            } else {
                throw exception
            }
        }

    /** Contains the camera's MAC address and IP respectively. */
    val cameraConnectionDetail = preference.map {
        Pair(it.cameraMac, it.cameraIp)
    }

    /**
     * Save thermal camera's connection related info to the local storage.
     *
     * @param mac The camera's MAC address. Remove the semicolons.
     * @param ip The camera's IP address.
     */
    @Suppress("UsePropertyAccessSyntax") // Data store needs to use generated setter
    suspend fun saveCameraConnectionDetails(mac: String, ip: String) {
        require(!mac.contains(":")) { "The camera's MAC address should not include the \":\"" }

        savePreference {
            setCameraIp(ip)
            setCameraMac(mac)
        }
    }

    /**
     * Saves data into the application's settings.
     *
     * @param block Data changes to be made.
     */
    private suspend fun savePreference(block: Preference.Builder.() -> Preference.Builder) {
        dataStore.updateData {
            it.toBuilder().apply { block(this) }.build()
        }
    }

    companion object {
        val TAG = PreferenceRepository::class.simpleName
        const val FILE_NAME = "preference"
    }
}