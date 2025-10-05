package com.furinlab.eightman.uuid_gen.domain.model

data class UuidFormatOptions(
    val hyphenated: Boolean = true,
    val uppercase: Boolean = false,
    val braces: Boolean = false,
) {
    fun encode(): Int {
        var result = 0
        if (hyphenated) result = result or FLAG_HYPHEN
        if (uppercase) result = result or FLAG_UPPERCASE
        if (braces) result = result or FLAG_BRACES
        return result
    }

    fun apply(value: String): String {
        var processed = if (hyphenated) value else value.replace("-", "")
        processed = if (uppercase) processed.uppercase() else processed.lowercase()
        return if (braces) "{$processed}" else processed
    }

    companion object {
        private const val FLAG_HYPHEN = 1
        private const val FLAG_UPPERCASE = 1 shl 1
        private const val FLAG_BRACES = 1 shl 2

        fun decode(flags: Int?): UuidFormatOptions {
            val value = flags ?: 0
            return UuidFormatOptions(
                hyphenated = value and FLAG_HYPHEN != 0,
                uppercase = value and FLAG_UPPERCASE != 0,
                braces = value and FLAG_BRACES != 0,
            )
        }
    }
}
