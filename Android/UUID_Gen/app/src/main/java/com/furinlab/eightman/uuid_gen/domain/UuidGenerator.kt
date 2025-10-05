package com.furinlab.eightman.uuid_gen.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

sealed class UuidGenerationException(message: String) : Exception(message)
class InvalidNamespaceException : UuidGenerationException("名前空間 UUID が不正です")
class InvalidNameException : UuidGenerationException("Name を入力してください")

object UuidGenerator {
    private val random = SecureRandom()

    fun generate(
        version: UuidVersion,
        namespaceString: String?,
        name: String?,
        now: Instant = Instant.now()
    ): GeneratedUuid {
        return when (version) {
            UuidVersion.V4 -> GeneratedUuid(value = UUID.randomUUID(), version = UuidVersion.V4, createdAt = now)
            UuidVersion.V5 -> {
                val namespace = namespaceString?.takeIf { it.isNotBlank() }?.let {
                    runCatching { UUID.fromString(it) }.getOrElse { throw InvalidNamespaceException() }
                } ?: throw InvalidNamespaceException()
                val nonEmptyName = name?.takeIf { it.isNotBlank() } ?: throw InvalidNameException()
                val uuid = uuidV5(namespace, nonEmptyName)
                GeneratedUuid(value = uuid, version = UuidVersion.V5, createdAt = now, namespace = namespace, name = nonEmptyName)
            }
            UuidVersion.V7 -> {
                val uuid = uuidV7(now.toEpochMilli())
                GeneratedUuid(value = uuid, version = UuidVersion.V7, createdAt = now)
            }
        }
    }

    fun uuidV5(namespace: UUID, name: String): UUID {
        val namespaceBytes = namespace.toBytes()
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-1")
        digest.update(namespaceBytes)
        digest.update(nameBytes)
        val hash = digest.digest()
        val bytes = hash.copyOf(16)
        bytes[6] = (bytes[6].toInt() and 0x0F or 0x50).toByte()
        bytes[8] = (bytes[8].toInt() and 0x3F or 0x80).toByte()
        return uuidFromByteArray(bytes)
    }

    fun uuidV7(nowMillis: Long): UUID {
        val randomBytes = ByteArray(10)
        random.nextBytes(randomBytes)
        val bytes = ByteArray(16)
        bytes[0] = (nowMillis shr 40 and 0xFF).toByte()
        bytes[1] = (nowMillis shr 32 and 0xFF).toByte()
        bytes[2] = (nowMillis shr 24 and 0xFF).toByte()
        bytes[3] = (nowMillis shr 16 and 0xFF).toByte()
        bytes[4] = (nowMillis shr 8 and 0xFF).toByte()
        bytes[5] = (nowMillis and 0xFF).toByte()
        bytes[6] = (randomBytes[0].toInt() and 0x0F or 0x70).toByte()
        bytes[7] = randomBytes[1]
        bytes[8] = (randomBytes[2].toInt() and 0x3F or 0x80).toByte()
        bytes[9] = randomBytes[3]
        for (i in 0 until 6) {
            bytes[10 + i] = randomBytes[4 + i]
        }
        return uuidFromByteArray(bytes)
    }
}

private fun UUID.toBytes(): ByteArray {
    val msb = mostSignificantBits
    val lsb = leastSignificantBits
    val buffer = ByteArray(16)
    for (i in 0 until 8) {
        buffer[i] = (msb shr (56 - i * 8) and 0xFF).toByte()
    }
    for (i in 0 until 8) {
        buffer[8 + i] = (lsb shr (56 - i * 8) and 0xFF).toByte()
    }
    return buffer
}

private fun uuidFromByteArray(bytes: ByteArray): UUID {
    require(bytes.size == 16)
    var msb = 0L
    var lsb = 0L
    for (i in 0 until 8) {
        msb = msb shl 8 or (bytes[i].toLong() and 0xFF)
    }
    for (i in 8 until 16) {
        lsb = lsb shl 8 or (bytes[i].toLong() and 0xFF)
    }
    return UUID(msb, lsb)
}
