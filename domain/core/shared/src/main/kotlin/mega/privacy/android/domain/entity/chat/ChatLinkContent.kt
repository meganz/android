package mega.privacy.android.domain.entity.chat

/**
 * Chat link content
 *
 * @property link Chat link
 * @property chatHandle Chat id
 */
sealed interface ChatLinkContent {

    val link: String
    val chatHandle: Long

    /**
     * Meeting link
     *
     * @property isInThisMeeting
     * @property handles
     * @property text
     * @property userHandle
     * @property exist
     * @property isWaitingRoom
     */
    data class MeetingLink(
        override val link: String,
        override val chatHandle: Long,
        val isInThisMeeting: Boolean,
        val handles: List<Long>?,
        val text: String,
        val userHandle: Long,
        val exist: Boolean,
        val isWaitingRoom: Boolean,
    ) : ChatLinkContent

    /**
     * Chat link
     */
    data class ChatLink(
        override val link: String,
        override val chatHandle: Long,
    ) : ChatLinkContent
}