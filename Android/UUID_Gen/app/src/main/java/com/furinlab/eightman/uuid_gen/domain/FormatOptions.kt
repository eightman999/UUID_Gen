package com.furinlab.eightman.uuid_gen.domain

import java.util.UUID

@JvmInline
value class FormatOptions(val raw: Long) {
    fun contains(option: FormatOption): Boolean = raw and option.mask != 0L

    fun toggle(option: FormatOption, enabled: Boolean): FormatOptions {
        return if (enabled) {
            FormatOptions(raw or option.mask)
        } else {
            FormatOptions(raw and option.mask.inv())
        }
    }

    fun format(uuid: UUID): String {
        var string = uuid.toString()
        if (!contains(FormatOption.HYPHEN)) {
            string = string.replace("-", "")
        }
        string = if (contains(FormatOption.UPPERCASE)) {
            string.uppercase()
        } else {
            string.lowercase()
        }
        if (contains(FormatOption.BRACES)) {
            string = "{$string}"
        }
        return string
    }

    companion object {
        val DEFAULT = FormatOptions(FormatOption.HYPHEN.mask)
    }
}

enum class FormatOption(val mask: Long) {
    HYPHEN(1L shl 0),
    UPPERCASE(1L shl 1),
    BRACES(1L shl 2);
}
