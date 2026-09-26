package com.apexus.storagelens.ui.util

import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import java.text.DateFormat
import java.util.Date

/** Taille lisible et localisée (« 1,2 Go », « 340 Mo »…). */
@Composable
@ReadOnlyComposable
fun formatSize(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes.coerceAtLeast(0))

fun formatDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

fun formatRelative(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

fun percent(part: Long, total: Long): Float = if (total <= 0) 0f else (part.toFloat() / total).coerceIn(0f, 1f)
