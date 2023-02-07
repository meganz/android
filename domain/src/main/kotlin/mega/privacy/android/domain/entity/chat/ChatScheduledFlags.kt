package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled flags.
 *
 * @property isEmailsDisabled    Flag to disable emails sending.
 * @property isEmpty             True, not flags. False, otherwise.
 */
data class ChatScheduledFlags(
    val isEmailsDisabled: Boolean,
    val isEmpty: Boolean,
)
