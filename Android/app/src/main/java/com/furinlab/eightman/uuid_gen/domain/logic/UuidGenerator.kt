package com.furinlab.eightman.uuid_gen.domain.logic

import com.furinlab.eightman.uuid_gen.domain.model.UuidVersion
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import kotlin.math.min

object UuidGenerator {
    private val random = SecureRandom()

    fun generate(
        version: UuidVersion,
        namespace: UUID? = null,
        name: String? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): UUID {
        return when (version) {
            UuidVersion.V4 -> UUID.randomUUID()
            UuidVersion.V5 -> {
                require(namespace != null) { "Namespace UUID is required for v5" }
                require(!name.isNullOrBlank()) { "Name is required for v5" }
                uuidV5(namespace, name)
            }
            UuidVersion.V7 -> uuidV7(nowMs)
        }
    }

    fun uuidV5(namespace: UUID, name: String): UUID {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(toBytes(namespace))
        md.update(name.toByteArray(Charsets.UTF_8))
        val hash = md.digest()
        val bytes = hash.copyOfRange(0, 16)
        bytes[6] = (bytes[6].toInt() and 0x0f or (5 shl 4)).toByte()
        bytes[8] = (bytes[8].toInt() and 0x3f or 0x80).toByte()
        return fromBytes(bytes)
    }

    fun uuidV7(nowMs: Long): UUID {
        val timestamp = nowMs and 0xFFFFFFFFFFFFL
        val randA = random.nextInt(1 shl 12)
        val randB = random.nextLong() and 0x3FFFFFFFFFFFFFFFL

        val msb = (timestamp shl 16) or (0x7L shl 12) or randA.toLong()
        val lsb = randB or 0x8000000000000000L
        return UUID(msb, lsb)
    }

    private fun toBytes(uuid: UUID): ByteArray {
        val buffer = ByteBuffer.wrap(ByteArray(16))
        buffer.putLong(uuid.mostSignificantBits)
        buffer.putLong(uuid.leastSignificantBits)
        return buffer.array()
    }

    private fun fromBytes(bytes: ByteArray): UUID {
        require(bytes.size >= 16) { "UUID byte array must have at least 16 bytes" }
        val buffer = ByteBuffer.wrap(bytes, 0, min(bytes.size, 16))
        val msb = buffer.long
        val lsb = buffer.long
        return UUID(msb, lsb)
    }
}
