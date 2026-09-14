package com.asmr.player.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MessageManagerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun tryConsume_allowsEachMessageOnlyOnce() {
        val manager = MessageManager(context)

        assertTrue(manager.tryConsume(1L))
        assertFalse(manager.tryConsume(1L))
        assertFalse(manager.tryConsume(0L))
        assertTrue(manager.tryConsume(2L))
        assertFalse(manager.tryConsume(2L))
    }
}
