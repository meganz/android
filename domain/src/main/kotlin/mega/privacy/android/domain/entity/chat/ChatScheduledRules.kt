package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled rules
 *
 * @property freq
 * @property interval
 * @property until
 */
data class ChatScheduledRules(
    val freq: Int,
    val interval: Int,
    val until: String,
)
