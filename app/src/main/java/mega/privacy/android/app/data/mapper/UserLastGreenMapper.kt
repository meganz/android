package mega.privacy.android.app.data.mapper

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.getFormattedStringOrDefault
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.domain.entity.user.UserLastGreen
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Mapper to convert data to [UserLastGreen]
 */
typealias UserLastGreenMapper = (
    @JvmSuppressWildcards Context,
    @JvmSuppressWildcards Long,
    @JvmSuppressWildcards Int,
) -> UserLastGreen

internal fun toUserUserLastGreen(context: Context, handle: Long, lastGreen: Int): UserLastGreen =
    UserLastGreen(handle, getLastSeen(context, lastGreen))

private fun getLastSeen(context: Context, lastGreen: Int): String {
    val calGreen = Calendar.getInstance().apply { add(Calendar.MINUTE, lastGreen) }
    val calToday = Calendar.getInstance()
    val tc = TimeUtils(TimeUtils.DATE)
    val ts = calGreen.timeInMillis
    val timeToConsiderAsLongTimeAgo = 65535

    Timber.d("Ts last green: %s", ts)

    return when {
        lastGreen >= timeToConsiderAsLongTimeAgo -> {
            context.getFormattedStringOrDefault(R.string.last_seen_long_time_ago)
        }
        tc.compare(calGreen, calToday) == 0 -> {
            val tz = calGreen.timeZone
            val df = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
            val time = df.format(calGreen.time)
            context.getFormattedStringOrDefault(R.string.last_seen_today, time)
        }
        else -> {
            val tz = calGreen.timeZone
            var df = SimpleDateFormat("HH:mm", Locale.getDefault()).apply { timeZone = tz }
            val time = df.format(calGreen.time)
            df = SimpleDateFormat("dd MMM", Locale.getDefault())
            val day = df.format(calGreen.time)
            context.getFormattedStringOrDefault(R.string.last_seen_general, day, time)
        }
    }.replace("[A]", "").replace("[/A]", "")
}