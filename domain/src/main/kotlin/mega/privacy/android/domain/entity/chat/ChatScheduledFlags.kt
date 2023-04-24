package mega.privacy.android.domain.entity.chat

/**
 * Chat scheduled flags.
 *
 * @property sendEmails         Flag to send emails.
 * @property isEmpty            True, not flags. False, otherwise.
 */
data class ChatScheduledFlags(
    val sendEmails: Boolean,
    val isEmpty: Boolean,
)
