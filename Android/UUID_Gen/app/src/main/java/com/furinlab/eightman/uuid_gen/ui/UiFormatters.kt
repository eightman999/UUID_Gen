package com.furinlab.eightman.uuid_gen.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM)
    .withLocale(Locale.getDefault())
    .withZone(ZoneId.systemDefault())

private val timeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofLocalizedTime(FormatStyle.SHORT)
    .withLocale(Locale.getDefault())
    .withZone(ZoneId.systemDefault())

fun formatDateTime(instant: Instant): String = dateTimeFormatter.format(instant)

fun formatTime(instant: Instant): String = timeFormatter.format(instant)

fun shareText(context: Context, value: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, value)
    }
    val chooser = Intent.createChooser(sendIntent, "UUID を共有")
    runCatching { context.startActivity(chooser) }
        .onFailure { Toast.makeText(context, "共有できませんでした", Toast.LENGTH_SHORT).show() }
}
