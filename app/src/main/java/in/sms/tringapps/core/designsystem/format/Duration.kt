package `in`.sms.tringapps.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import `in`.sms.tringapps.R

/** Human readable duration split, rendered with localized string resources. */
sealed interface DurationLabel {
    data class HoursMinutes(val hours: Long, val minutes: Long) : DurationLabel
    data class Minutes(val minutes: Long) : DurationLabel
    data class Seconds(val seconds: Long) : DurationLabel
}

/** `null` when the duration is unknown (0 or negative), e.g. for series containers. */
fun durationLabelOf(totalSeconds: Long): DurationLabel? = when {
    totalSeconds <= 0 -> null
    totalSeconds < 60 -> DurationLabel.Seconds(totalSeconds)
    totalSeconds < 3600 -> DurationLabel.Minutes(totalSeconds / 60)
    else -> DurationLabel.HoursMinutes(totalSeconds / 3600, (totalSeconds % 3600) / 60)
}

@Composable
fun durationText(totalSeconds: Long): String? = when (val label = durationLabelOf(totalSeconds)) {
    null -> null
    is DurationLabel.Seconds -> stringResource(R.string.duration_seconds, label.seconds.toInt())
    is DurationLabel.Minutes -> stringResource(R.string.duration_minutes, label.minutes.toInt())
    is DurationLabel.HoursMinutes ->
        stringResource(R.string.duration_hours_minutes, label.hours.toInt(), label.minutes.toInt())
}
