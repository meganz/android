package mega.privacy.android.app.presentation.meeting.managechathistory.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

internal enum class ChatHistoryRetentionOption(@StringRes val stringId: Int) {
    Disabled(R.string.history_retention_option_disabled),
    Day(R.string.history_retention_option_one_day),
    Week(R.string.history_retention_option_one_week),
    Month(R.string.history_retention_option_one_month),
    Custom(R.string.history_retention_option_custom)
}
