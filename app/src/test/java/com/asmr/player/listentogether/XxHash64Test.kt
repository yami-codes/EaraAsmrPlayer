package com.asmr.player.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class XxHash64Test {

    @Test
    fun hashHex_isStableForSameInput() {
        val input = "RJ123456-track-a.flac-1234567".encodeToByteArray()

        val first = XxHash64.hashHex(input)
        val second = XxHash64.hashHex(input)

        assertEquals(first, second)
        assertEquals(16, first.length)
    }

    @Test
    fun hashHex_changesWhenInputChanges() {
        val first = XxHash64.hashHex("track-a".encodeToByteArray())
        val second = XxHash64.hashHex("track-b".encodeToByteArray())

        assertNotEquals(first, second)
    }

    @Test
    fun streamHash_matchesByteArrayHash() {
        val input = ByteArray(8192) { index -> (index * 31 % 251).toByte() }

        val byteArrayHash = XxHash64.hashHex(input)
        val streamHash = XxHash64.hashStreamHex(ByteArrayInputStream(input))

        assertEquals(byteArrayHash, streamHash)
    }
}
