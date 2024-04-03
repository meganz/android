package mega.privacy.android.app.presentation.meeting.managechathistory.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * Represents the chat history retention time options
 *
 * @param stringId The string resource ID represents an option's display text.
 */
enum class ChatHistoryRetentionOption(@StringRes val stringId: Int) {
    /**
     * The chat history will not be removed.
     */
    Disabled(R.string.history_retention_option_disabled),

    /**
     * The chat history will be removed after one day.
     */
    OneDay(R.string.history_retention_option_one_day),

    /**
     * The chat history will be removed after one week.
     */
    OneWeek(R.string.history_retention_option_one_week),

    /**
     * The chat history will be removed after one month.
     */
    OneMonth(R.string.history_retention_option_one_month),

    /**
     * The chat history will be removed by a custom time set by the user.
     */
    Custom(R.string.history_retention_option_custom)
}
