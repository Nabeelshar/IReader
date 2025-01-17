package org.ireader.common_extensions

import android.text.format.DateUtils
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaLocalDateTime
import org.ireader.core_ui.preferences.PreferenceValues
import java.text.DateFormat
import java.util.Date

fun Date.toDateTimestampString(dateFormatter: DateFormat): String {
    val date = dateFormatter.format(this)
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
    return "$date $time"
}

fun Date.toTimestampString(): String {
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(this)
}
fun LocalDate.asRelativeTimeString(
    range: PreferenceValues.RelativeTime = PreferenceValues.RelativeTime.Day,
    dateFormat:String = "",
): String {
    val rangeFormat = when (range) {
        PreferenceValues.RelativeTime.Seconds, -> DateUtils.SECOND_IN_MILLIS
        PreferenceValues.RelativeTime.Minutes -> DateUtils.MINUTE_IN_MILLIS
        PreferenceValues.RelativeTime.Hour -> DateUtils.HOUR_IN_MILLIS
        PreferenceValues.RelativeTime.Day -> DateUtils.DAY_IN_MILLIS
        PreferenceValues.RelativeTime.Week -> DateUtils.WEEK_IN_MILLIS
        PreferenceValues.RelativeTime.Off -> null
    }
    return DateUtils
        .getRelativeTimeSpanString(
            atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            System.currentTimeMillis(),
            rangeFormat?:DateUtils.DAY_IN_MILLIS
        )
        .toString()
}

class DateTimeFormatter constructor(pattern: String) {
    internal val jtFormatter = java.time.format.DateTimeFormatter.ofPattern(pattern)
}

fun LocalDateTime.format(formatter: DateTimeFormatter): String {
    return toJavaLocalDateTime().format(formatter.jtFormatter)
}
