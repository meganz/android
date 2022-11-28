package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled flags.
 *
 * @property isEmailsDisabled             Flag to disable emails sending.
 */
data class ChatScheduledFlags(
    val isEmailsDisabled: Boolean,
)
