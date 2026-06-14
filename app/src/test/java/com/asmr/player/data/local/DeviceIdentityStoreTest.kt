package com.asmr.player.data.local

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class DeviceIdentityStoreTest {
    private val context = RuntimeEnvironment.getApplication()
    private val prefs = context.getSharedPreferences("device_identity", Context.MODE_PRIVATE)

    @Before
    fun setUp() {
        prefs.edit().clear().commit()
    }

    @After
    fun tearDown() {
        prefs.edit().clear().commit()
    }

    @Test
    fun getOrCreateDeviceId_reusesStoredId() {
        val firstStore = DeviceIdentityStore(context)
        val firstId = firstStore.getOrCreateDeviceId()
        UUID.fromString(firstId)

        val secondStore = DeviceIdentityStore(context)
        val secondId = secondStore.getOrCreateDeviceId()

        assertEquals(firstId, secondId)
        assertEquals(firstId, prefs.getString("device_id", null))
    }

    @Test
    fun getOrCreateDeviceId_replacesInvalidStoredId() {
        prefs.edit().putString("device_id", "broken").commit()

        val deviceId = DeviceIdentityStore(context).getOrCreateDeviceId()
        UUID.fromString(deviceId)

        assertNotEquals("broken", deviceId)
        assertEquals(deviceId, prefs.getString("device_id", null))
    }
}
