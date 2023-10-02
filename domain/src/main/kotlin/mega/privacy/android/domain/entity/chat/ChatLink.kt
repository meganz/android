package mega.privacy.android.domain.entity.chat

/**
 * Chat link content
 *
 * @property link
 */
sealed interface ChatLinkContent {
    val link: String

    /**
     * Meeting link
     *
     * @property link
     * @property chatHandle
     * @property isInThisMeeting
     * @property handles
     * @property text
     * @property userHandle
     * @property exist
     * @property isWaitingRoom
     */
    data class MeetingLink(
        override val link: String,
        val chatHandle: Long,
        val isInThisMeeting: Boolean,
        val handles: List<Long>?,
        val text: String,
        val userHandle: Long,
        val exist: Boolean,
        val isWaitingRoom: Boolean,
    ) : ChatLinkContent

    /**
     * Chat link
     *
     * @property link
     */
    data class ChatLink(
        override val link: String,
    ) : ChatLinkContent
}