package com.example.cloudide.util

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date

/** Localized relative time such as "10 min. ago" (follows the phone language). */
fun relativeTime(millis: Long): String =
    if (millis <= 0L) {
        ""
    } else {
        DateUtils.getRelativeTimeSpanString(
            millis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

fun formatClock(millis: Long): String =
    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))