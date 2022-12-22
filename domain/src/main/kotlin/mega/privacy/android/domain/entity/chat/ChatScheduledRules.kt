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
    val interval: Int? = null,
    val until: String? = null,
)
