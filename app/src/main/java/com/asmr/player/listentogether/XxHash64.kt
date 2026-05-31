package com.asmr.player.listentogether

import java.io.InputStream

internal object XxHash64 {
    private const val PRIME64_1 = -7046029288634856825L
    private const val PRIME64_2 = -4417276706812531889L
    private const val PRIME64_3 = 1609587929392839161L
    private const val PRIME64_4 = -8796714831421723037L
    private const val PRIME64_5 = 2870177450012600261L

    fun hash(input: ByteArray, seed: Long = 0L): Long {
        val length = input.size
        var index = 0
        val end = length
        val hash = if (length >= 32) {
            var v1 = seed + PRIME64_1 + PRIME64_2
            var v2 = seed + PRIME64_2
            var v3 = seed
            var v4 = seed - PRIME64_1
            val limit = end - 32
            while (index <= limit) {
                v1 = round(v1, littleEndianLong(input, index))
                index += 8
                v2 = round(v2, littleEndianLong(input, index))
                index += 8
                v3 = round(v3, littleEndianLong(input, index))
                index += 8
                v4 = round(v4, littleEndianLong(input, index))
                index += 8
            }
            mergeRound(mergeRound(mergeRound(mergeRound(rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) + rotateLeft(v4, 18), v1), v2), v3), v4)
        } else {
            seed + PRIME64_5
        }

        var h64 = hash + length.toLong()
        while (index <= end - 8) {
            val k1 = round(0L, littleEndianLong(input, index))
            h64 = rotateLeft(h64 xor k1, 27) * PRIME64_1 + PRIME64_4
            index += 8
        }
        if (index <= end - 4) {
            h64 = rotateLeft(h64 xor ((littleEndianInt(input, index).toLong() and 0xFFFF_FFFFL) * PRIME64_1), 23) * PRIME64_2 + PRIME64_3
            index += 4
        }
        while (index < end) {
            h64 = rotateLeft(h64 xor ((input[index].toLong() and 0xFFL) * PRIME64_5), 11) * PRIME64_1
            index += 1
        }
        return avalanche(h64)
    }

    fun hashHex(input: ByteArray, seed: Long = 0L): String {
        return java.lang.Long.toUnsignedString(hash(input, seed), 16).padStart(16, '0')
    }

    fun hashStream(inputStream: InputStream, seed: Long = 0L): Long {
        val state = StreamingState(seed)
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = inputStream.read(buffer)
            if (read <= 0) break
            state.update(buffer, 0, read)
        }
        return state.digest()
    }

    fun hashStreamHex(inputStream: InputStream, seed: Long = 0L): String {
        return java.lang.Long.toUnsignedString(hashStream(inputStream, seed), 16).padStart(16, '0')
    }

    fun hashStreamHexWithSize(
        inputStreamFactory: () -> InputStream?,
        fileSizeBytes: Long,
        prefixBytes: Int = 10240
    ): String {
        val sizeBytes = ByteArray(8)
        sizeBytes[0] = (fileSizeBytes ushr 0).toByte()
        sizeBytes[1] = (fileSizeBytes ushr 8).toByte()
        sizeBytes[2] = (fileSizeBytes ushr 16).toByte()
        sizeBytes[3] = (fileSizeBytes ushr 24).toByte()
        sizeBytes[4] = (fileSizeBytes ushr 32).toByte()
        sizeBytes[5] = (fileSizeBytes ushr 40).toByte()
        sizeBytes[6] = (fileSizeBytes ushr 48).toByte()
        sizeBytes[7] = (fileSizeBytes ushr 56).toByte()

        val state = StreamingState(0L)
        state.update(sizeBytes, 0, 8)

        val contentBytes = fileSizeBytes.coerceAtMost(prefixBytes.toLong()).toInt()
        val stream = inputStreamFactory()
        if (stream != null) {
            stream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var remaining = contentBytes
                while (remaining > 0) {
                    val read = input.read(buffer, 0, remaining.coerceAtMost(buffer.size))
                    if (read <= 0) break
                    state.update(buffer, 0, read)
                    remaining -= read
                }
            }
        }

        return java.lang.Long.toUnsignedString(state.digest(), 16).padStart(16, '0')
    }

    private fun round(acc: Long, input: Long): Long {
        var value = acc + (input * PRIME64_2)
        value = rotateLeft(value, 31)
        value *= PRIME64_1
        return value
    }

    private fun mergeRound(acc: Long, value: Long): Long {
        var result = acc xor round(0L, value)
        result = result * PRIME64_1 + PRIME64_4
        return result
    }

    private fun avalanche(hash: Long): Long {
        var value = hash
        value = value xor (value ushr 33)
        value *= PRIME64_2
        value = value xor (value ushr 29)
        value *= PRIME64_3
        value = value xor (value ushr 32)
        return value
    }

    private fun littleEndianLong(data: ByteArray, index: Int): Long {
        return (data[index].toLong() and 0xFFL) or
            ((data[index + 1].toLong() and 0xFFL) shl 8) or
            ((data[index + 2].toLong() and 0xFFL) shl 16) or
            ((data[index + 3].toLong() and 0xFFL) shl 24) or
            ((data[index + 4].toLong() and 0xFFL) shl 32) or
            ((data[index + 5].toLong() and 0xFFL) shl 40) or
            ((data[index + 6].toLong() and 0xFFL) shl 48) or
            ((data[index + 7].toLong() and 0xFFL) shl 56)
    }

    private fun littleEndianInt(data: ByteArray, index: Int): Int {
        return (data[index].toInt() and 0xFF) or
            ((data[index + 1].toInt() and 0xFF) shl 8) or
            ((data[index + 2].toInt() and 0xFF) shl 16) or
            ((data[index + 3].toInt() and 0xFF) shl 24)
    }

    private fun rotateLeft(value: Long, distance: Int): Long =
        (value shl distance) or (value ushr (64 - distance))

    private class StreamingState(private val seed: Long) {
        private val buffer = ByteArray(32)
        private var bufferSize = 0
        private var totalLength = 0L
        private var v1 = seed + PRIME64_1 + PRIME64_2
        private var v2 = seed + PRIME64_2
        private var v3 = seed
        private var v4 = seed - PRIME64_1

        fun update(data: ByteArray, offset: Int, length: Int) {
            if (length <= 0) return
            var index = offset
            var remaining = length
            totalLength += length.toLong()

            if (bufferSize + remaining < 32) {
                data.copyInto(buffer, destinationOffset = bufferSize, startIndex = index, endIndex = index + remaining)
                bufferSize += remaining
                return
            }

            if (bufferSize > 0) {
                val needed = 32 - bufferSize
                data.copyInto(buffer, destinationOffset = bufferSize, startIndex = index, endIndex = index + needed)
                process(buffer, 0)
                index += needed
                remaining -= needed
                bufferSize = 0
            }

            while (remaining >= 32) {
                process(data, index)
                index += 32
                remaining -= 32
            }

            if (remaining > 0) {
                data.copyInto(buffer, destinationOffset = 0, startIndex = index, endIndex = index + remaining)
                bufferSize = remaining
            }
        }

        fun digest(): Long {
            var hash = if (totalLength >= 32) {
                val base = rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) + rotateLeft(v4, 18)
                mergeRound(mergeRound(mergeRound(mergeRound(base, v1), v2), v3), v4)
            } else {
                seed + PRIME64_5
            }

            hash += totalLength
            var index = 0
            while (index <= bufferSize - 8) {
                val k1 = round(0L, littleEndianLong(buffer, index))
                hash = rotateLeft(hash xor k1, 27) * PRIME64_1 + PRIME64_4
                index += 8
            }
            if (index <= bufferSize - 4) {
                hash = rotateLeft(hash xor ((littleEndianInt(buffer, index).toLong() and 0xFFFF_FFFFL) * PRIME64_1), 23) * PRIME64_2 + PRIME64_3
                index += 4
            }
            while (index < bufferSize) {
                hash = rotateLeft(hash xor ((buffer[index].toLong() and 0xFFL) * PRIME64_5), 11) * PRIME64_1
                index += 1
            }
            return avalanche(hash)
        }

        private fun process(data: ByteArray, offset: Int) {
            var index = offset
            v1 = round(v1, littleEndianLong(data, index))
            index += 8
            v2 = round(v2, littleEndianLong(data, index))
            index += 8
            v3 = round(v3, littleEndianLong(data, index))
            index += 8
            v4 = round(v4, littleEndianLong(data, index))
        }
    }
}
