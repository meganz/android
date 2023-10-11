package mega.privacy.android.app.presentation.settings.passcode.model

import android.content.Context
import mega.privacy.android.app.R

/**
 * Timeout option
 *
 * @constructor Create empty Timeout option
 */
sealed interface TimeoutOption {
    /**
     * Get title
     *
     * @param context
     * @return the title
     */
    fun getTitle(context: Context): String

    /**
     * Get timeout in milliseconds
     *
     * @return the timeoutInMilliseconds
     */
    fun getTimeoutInMilliseconds(): Long

    /**
     * Immediate
     */
    data object Immediate : TimeoutOption {
        override fun getTitle(context: Context) =
            context.getString(R.string.action_immediately)

        override fun getTimeoutInMilliseconds() = 0L
    }


    /**
     * Seconds TimeSpan
     *
     * @property timeoutInSeconds
     */
    data class SecondsTimeSpan(val timeoutInSeconds: Int) : TimeoutOption {
        override fun getTitle(context: Context) = removeFormatPlaceholder(
            context.resources.getQuantityString(
                R.plurals.plural_call_ended_messages_seconds,
                timeoutInSeconds,
                timeoutInSeconds,
            )
        )

        override fun getTimeoutInMilliseconds() = timeoutInSeconds * 1000L
    }

    /**
     * Minutes TimeSpan
     *
     * @property timeoutInMinutes
     */
    data class MinutesTimeSpan(val timeoutInMinutes: Int) : TimeoutOption {
        override fun getTitle(context: Context) = removeFormatPlaceholder(
            context.resources.getQuantityString(
                R.plurals.plural_call_ended_messages_seconds,
                timeoutInMinutes,
                timeoutInMinutes,
            )
        )

        override fun getTimeoutInMilliseconds() = timeoutInMinutes * 60 * 1000L
    }
}

private fun removeFormatPlaceholder(text: String) = runCatching {
    text.replace("[A]", "")
        .replace("[/A]", "")
        .replace("[B]", "")
        .replace("[/B]", "")
        .replace("[C]", "")
        .replace("[/C]", "")
}.getOrDefault(text)
