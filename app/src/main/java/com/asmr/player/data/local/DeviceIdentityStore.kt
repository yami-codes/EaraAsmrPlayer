package com.asmr.player.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdentityStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var cachedDeviceId: String? = null

    fun getOrCreateDeviceId(): String {
        cachedDeviceId?.let { return it }
        return synchronized(this) {
            cachedDeviceId?.let { return@synchronized it }
            val deviceId = prefs.getString(KEY_DEVICE_ID, null)
                ?.takeIf(::isValidDeviceId)
                ?: createAndStoreDeviceId()
            cachedDeviceId = deviceId
            deviceId
        }
    }

    private fun createAndStoreDeviceId(): String {
        val deviceId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).commit()
        return deviceId
    }

    private companion object {
        const val PREFS_NAME = "device_identity"
        const val KEY_DEVICE_ID = "device_id"

        fun isValidDeviceId(value: String): Boolean {
            return runCatching { UUID.fromString(value) }.isSuccess
        }
    }
}
