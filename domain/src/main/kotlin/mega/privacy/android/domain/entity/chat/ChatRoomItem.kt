package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.ChatRoomLastMessage
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Chat room item
 *
 * @property chatId
 * @property title
 * @property lastMessage
 * @property lastMessageType
 * @property currentCallStatus
 * @property unreadCount
 * @property hasPermissions
 * @property isActive
 * @property isMuted
 * @property isArchived
 * @property lastTimestamp
 * @property lastTimestampFormatted
 * @property highlight
 * @property header
 * @property call
 * @constructor Create empty chat room item
 */
sealed class ChatRoomItem(
    open val call: ChatCall? = null,
    open val chatId: Long,
    open val title: String,
    open val lastMessage: String? = null,
    open val lastMessageType: ChatRoomLastMessage = ChatRoomLastMessage.Unknown,
    open val currentCallStatus: ChatRoomItemStatus = ChatRoomItemStatus.NotStarted,
    open val unreadCount: Int = 0,
    open val hasPermissions: Boolean = false,
    open val isActive: Boolean = false,
    open val isMuted: Boolean = false,
    open val isArchived: Boolean = false,
    open val lastTimestamp: Long = 0L,
    open val lastTimestampFormatted: String? = null,
    open val highlight: Boolean = false,
    open val header: String? = null,
    open val description: String = "",
    open val peers: List<Long> = emptyList(),
) {
    /**
     * Check if the last message is voice clip
     */
    val isLastMessageVoiceClip
        get() = lastMessageType == ChatRoomLastMessage.VoiceClip

    /**
     * Check if the last message is normal text
     */
    val isLastMessageNormal
        get() = lastMessageType == ChatRoomLastMessage.Normal

    /**
     * Individual chat room item
     *
     * @property userChatStatus
     * @property avatar
     * @property peerHandle
     * @property peerEmail
     * @property chatId
     * @property title
     * @property lastMessage
     * @property lastMessageType
     * @property currentCallStatus
     * @property unreadCount
     * @property hasPermissions
     * @property isActive
     * @property isMuted
     * @property isArchived
     * @property lastTimestamp
     * @property lastTimestampFormatted
     * @property highlight
     * @property header
     * @constructor Create empty Individual chat room item
     */
    data class IndividualChatRoomItem(
        val userChatStatus: UserChatStatus? = null,
        val avatar: ChatAvatarItem? = null,
        val peerHandle: Long? = null,
        val peerEmail: String? = null,
        override val call: ChatCall? = null,
        override val chatId: Long,
        override val title: String,
        override val lastMessage: String? = null,
        override val lastMessageType: ChatRoomLastMessage = ChatRoomLastMessage.Unknown,
        override val currentCallStatus: ChatRoomItemStatus = ChatRoomItemStatus.NotStarted,
        override val unreadCount: Int = 0,
        override val hasPermissions: Boolean = false,
        override val isActive: Boolean = false,
        override val isMuted: Boolean = false,
        override val isArchived: Boolean = false,
        override val lastTimestamp: Long = 0L,
        override val lastTimestampFormatted: String? = null,
        override val highlight: Boolean = false,
        override val header: String? = null,
        override val description: String = "",
        override val peers: List<Long> = emptyList(),
    ) : ChatRoomItem(
        call, chatId, title, lastMessage, lastMessageType, currentCallStatus,
        unreadCount, hasPermissions, isActive, isMuted, isArchived, lastTimestamp,
        lastTimestampFormatted, highlight, header, description, peers
    )

    /**
     * Note to self chat room item
     *
     * @property userChatStatus
     * @property avatar
     * @property peerHandle
     * @property peerEmail
     * @property chatId
     * @property title
     * @property lastMessage
     * @property lastMessageType
     * @property currentCallStatus
     * @property unreadCount
     * @property hasPermissions
     * @property isActive
     * @property isMuted
     * @property isArchived
     * @property lastTimestamp
     * @property lastTimestampFormatted
     * @property highlight
     * @property header
     * @constructor Create empty Individual chat room item
     */
    data class NoteToSelfChatRoomItem(
        val userChatStatus: UserChatStatus? = null,
        val avatar: ChatAvatarItem? = null,
        val peerHandle: Long? = null,
        val peerEmail: String? = null,
        override val call: ChatCall? = null,
        override val chatId: Long,
        override val title: String,
        override val lastMessage: String? = null,
        override val lastMessageType: ChatRoomLastMessage = ChatRoomLastMessage.Unknown,
        override val currentCallStatus: ChatRoomItemStatus = ChatRoomItemStatus.NotStarted,
        override val unreadCount: Int = 0,
        override val hasPermissions: Boolean = false,
        override val isActive: Boolean = false,
        override val isMuted: Boolean = false,
        override val isArchived: Boolean = false,
        override val lastTimestamp: Long = 0L,
        override val lastTimestampFormatted: String? = null,
        override val highlight: Boolean = false,
        override val header: String? = null,
        override val description: String = "",
        override val peers: List<Long> = emptyList(),
    ) : ChatRoomItem(
        call, chatId, title, lastMessage, lastMessageType, currentCallStatus,
        unreadCount, hasPermissions, isActive, isMuted, isArchived, lastTimestamp,
        lastTimestampFormatted, highlight, header, description, peers
    ) {
        /**
         * Check if the note to self chat room is empty
         */
        val isEmptyNoteToSelfChatRoom
            get():Boolean = lastMessageType == ChatRoomLastMessage.Invalid || lastMessageType == ChatRoomLastMessage.Unknown
    }

    /**
     * Group chat room item
     *
     * @property isPublic
     * @property avatars
     * @property chatId
     * @property title
     * @property lastMessage
     * @property lastMessageType
     * @property currentCallStatus
     * @property unreadCount
     * @property hasPermissions
     * @property isActive
     * @property isMuted
     * @property isArchived
     * @property lastTimestamp
     * @property lastTimestampFormatted
     * @property highlight
     * @property header
     * @constructor Create empty Group chat room item
     */
    data class GroupChatRoomItem(
        val isPublic: Boolean = false,
        val avatars: List<ChatAvatarItem>? = null,
        override val call: ChatCall? = null,
        override val chatId: Long,
        override val title: String,
        override val lastMessage: String? = null,
        override val lastMessageType: ChatRoomLastMessage = ChatRoomLastMessage.Unknown,
        override val currentCallStatus: ChatRoomItemStatus = ChatRoomItemStatus.NotStarted,
        override val unreadCount: Int = 0,
        override val hasPermissions: Boolean = false,
        override val isActive: Boolean = false,
        override val isMuted: Boolean = false,
        override val isArchived: Boolean = false,
        override val lastTimestamp: Long = 0L,
        override val lastTimestampFormatted: String? = null,
        override val highlight: Boolean = false,
        override val header: String? = null,
        override val description: String = "",
        override val peers: List<Long> = emptyList(),
    ) : ChatRoomItem(
        call, chatId, title, lastMessage, lastMessageType, currentCallStatus,
        unreadCount, hasPermissions, isActive, isMuted, isArchived, lastTimestamp,
        lastTimestampFormatted, highlight, header, description, peers
    )

    /**
     * Meeting chat room item
     *
     * @property schedId
     * @property isPending
     * @property isRecurringDaily
     * @property isRecurringWeekly
     * @property isRecurringMonthly
     * @property scheduledStartTimestamp
     * @property scheduledEndTimestamp
     * @property scheduledTimestampFormatted
     * @property isWaitingRoom
     * @property isPublic
     * @property avatars
     * @property chatId
     * @property title
     * @property lastMessage
     * @property lastMessageType
     * @property currentCallStatus
     * @property unreadCount
     * @property hasPermissions
     * @property isActive
     * @property isMuted
     * @property isArchived
     * @property lastTimestamp
     * @property lastTimestampFormatted
     * @property highlight
     * @property header
     * @property call
     * @constructor Create empty Meeting chat room item
     */
    data class MeetingChatRoomItem(
        val schedId: Long? = null,
        val isPending: Boolean = false,
        val isRecurringDaily: Boolean = false,
        val isRecurringWeekly: Boolean = false,
        val isRecurringMonthly: Boolean = false,
        val scheduledStartTimestamp: Long? = null,
        val scheduledEndTimestamp: Long? = null,
        val scheduledTimestampFormatted: String? = null,
        val isWaitingRoom: Boolean = false,
        val isPublic: Boolean = false,
        val avatars: List<ChatAvatarItem>? = null,
        val isCancelled: Boolean = false,
        override val call: ChatCall? = null,
        override val chatId: Long,
        override val title: String,
        override val lastMessage: String? = null,
        override val lastMessageType: ChatRoomLastMessage = ChatRoomLastMessage.Unknown,
        override val currentCallStatus: ChatRoomItemStatus = ChatRoomItemStatus.NotStarted,
        override val unreadCount: Int = 0,
        override val hasPermissions: Boolean = false,
        override val isActive: Boolean = false,
        override val isMuted: Boolean = false,
        override val isArchived: Boolean = false,
        override val lastTimestamp: Long = 0L,
        override val lastTimestampFormatted: String? = null,
        override val highlight: Boolean = false,
        override val header: String? = null,
        override val description: String = "",
        override val peers: List<Long> = emptyList(),
    ) : ChatRoomItem(
        call, chatId, title, lastMessage, lastMessageType, currentCallStatus,
        unreadCount, hasPermissions, isActive, isMuted, isArchived, lastTimestamp,
        lastTimestampFormatted, highlight, header, description, peers
    ) {

        /**
         * Check if Meeting is recurring
         *
         * @return  True if is a recurring meeting, false otherwise.
         */
        fun isRecurring(): Boolean =
            isRecurringDaily || isRecurringWeekly || isRecurringMonthly
    }

    /**
     * Check if chat is public
     *
     * @return  True if is public, false otherwise.
     */
    fun isPublicChat(): Boolean =
        this is GroupChatRoomItem && this.isPublic || this is MeetingChatRoomItem && this.isPublic

    /**
     * Check if chat is pending
     *
     * @return  True if is a pending meeting, false otherwise.
     */
    fun isPendingMeeting(): Boolean =
        this is MeetingChatRoomItem && this.isCancelled.not() && this.isPending

    /**
     * Check if chat is a recurring meeting
     *
     * @return  True if is a recurring meeting, false otherwise.
     */
    fun isRecurringMeeting(): Boolean =
        this is MeetingChatRoomItem && this.isRecurring()

    /**
     * Check if chat is a cancelled recurring meeting
     *
     * @return  True if is a cancelled recurring meeting, false otherwise.
     */
    fun isCancelledRecurringMeeting(): Boolean =
        this is MeetingChatRoomItem && this.isCancelled

    /**
     * Check if chat has ongoing call
     *
     * @return  True if has an ongoing call, false otherwise.
     */
    fun hasOngoingCall(): Boolean = currentCallStatus != ChatRoomItemStatus.NotStarted

    /**
     * Check if chat has call in progress
     *
     * @return  True if has an ongoing call, false otherwise.
     */
    fun hasCallInProgress(): Boolean = currentCallStatus == ChatRoomItemStatus.Joined

    /**
     * Get duration based in the initial timestamp
     *
     * @return [Duration]
     */
    fun getDurationFromInitialTimestamp(): Duration? {
        return call?.initialTimestamp?.takeIf { it != 0L }?.let {
            val currentDuration = Instant.now().epochSecond.toDuration(DurationUnit.SECONDS)
            val initialDuration = it.seconds
            currentDuration.minus(initialDuration)
        }
    }

    /**
     * Get chat room item avatars
     *
     * @return  List of [ChatAvatarItem]
     */
    fun getChatAvatars(): List<ChatAvatarItem>? = when (this) {
        is IndividualChatRoomItem -> avatar?.let(::listOf)
        is GroupChatRoomItem -> avatars
        is MeetingChatRoomItem -> avatars
        is NoteToSelfChatRoomItem -> null
    }

    /**
     * Returns a copy of this chat room item with the provided parameters.
     */
    fun copyChatRoomItem(
        call: ChatCall? = this.call,
        chatId: Long = this.chatId,
        title: String = this.title,
        lastMessage: String? = this.lastMessage,
        lastMessageType: ChatRoomLastMessage = this.lastMessageType,
        currentCallStatus: ChatRoomItemStatus = this.currentCallStatus,
        unreadCount: Int = this.unreadCount,
        hasPermissions: Boolean = this.hasPermissions,
        isActive: Boolean = this.isActive,
        isArchived: Boolean = this.isArchived,
        isMuted: Boolean = this.isMuted,
        lastTimestamp: Long = this.lastTimestamp,
        lastTimestampFormatted: String? = this.lastTimestampFormatted,
        highlight: Boolean = this.highlight,
        header: String? = this.header,
        isPublic: Boolean? = null,
        userChatStatus: UserChatStatus? = null,
        avatarItems: List<ChatAvatarItem>? = null,
        peerHandle: Long? = null,
        peerEmail: String? = null,
        schedId: Long? = null,
        isPending: Boolean? = null,
        isRecurringDaily: Boolean? = null,
        isRecurringWeekly: Boolean? = null,
        isRecurringMonthly: Boolean? = null,
        scheduledStartTimestamp: Long? = null,
        scheduledEndTimestamp: Long? = null,
        scheduledTimestampFormatted: String? = null,
        isWaitingRoom: Boolean? = null,
        isCancelled: Boolean? = null,
        description: String? = null,
    ): ChatRoomItem = when (this) {
        is IndividualChatRoomItem -> copy(
            call = call,
            chatId = chatId,
            title = title,
            lastMessage = lastMessage,
            lastMessageType = lastMessageType,
            currentCallStatus = currentCallStatus,
            unreadCount = unreadCount,
            hasPermissions = hasPermissions,
            isActive = isActive,
            isArchived = isArchived,
            isMuted = isMuted,
            lastTimestamp = lastTimestamp,
            lastTimestampFormatted = lastTimestampFormatted,
            highlight = highlight,
            header = header,
            userChatStatus = userChatStatus ?: this.userChatStatus,
            avatar = avatarItems?.firstOrNull() ?: this.avatar,
            peerHandle = peerHandle ?: this.peerHandle,
            peerEmail = peerEmail ?: this.peerEmail,
            description = description ?: this.description
        )

        is NoteToSelfChatRoomItem -> copy(
            call = null,
            chatId = chatId,
            title = title,
            lastMessage = lastMessage,
            lastMessageType = lastMessageType,
            currentCallStatus = currentCallStatus,
            unreadCount = 0,
            hasPermissions = hasPermissions,
            isActive = isActive,
            isArchived = isArchived,
            isMuted = false,
            lastTimestamp = lastTimestamp,
            lastTimestampFormatted = lastTimestampFormatted,
            highlight = highlight,
            header = header,
            userChatStatus = null,
            avatar = null,
            peerHandle = peerHandle ?: this.peerHandle,
            peerEmail = peerEmail ?: this.peerEmail,
            description = description ?: this.description
        )

        is GroupChatRoomItem -> copy(
            call = call,
            chatId = chatId,
            title = title,
            lastMessage = lastMessage,
            lastMessageType = lastMessageType,
            currentCallStatus = currentCallStatus,
            unreadCount = unreadCount,
            hasPermissions = hasPermissions,
            isActive = isActive,
            isArchived = isArchived,
            isMuted = isMuted,
            lastTimestamp = lastTimestamp,
            lastTimestampFormatted = lastTimestampFormatted,
            highlight = highlight,
            header = header,
            isPublic = isPublic ?: this.isPublic,
            avatars = avatarItems ?: this.avatars,
            description = description ?: this.description
        )

        is MeetingChatRoomItem -> copy(
            call = call,
            chatId = chatId,
            title = title,
            lastMessage = lastMessage,
            lastMessageType = lastMessageType,
            currentCallStatus = currentCallStatus,
            unreadCount = unreadCount,
            hasPermissions = hasPermissions,
            isActive = isActive,
            isArchived = isArchived,
            isMuted = isMuted,
            lastTimestamp = lastTimestamp,
            lastTimestampFormatted = lastTimestampFormatted,
            highlight = highlight,
            header = header,
            isWaitingRoom = isWaitingRoom ?: this.isWaitingRoom,
            isPublic = isPublic ?: this.isPublic,
            avatars = avatarItems ?: this.avatars,
            schedId = schedId ?: this.schedId,
            isPending = isPending ?: this.isPending,
            isRecurringDaily = isRecurringDaily ?: this.isRecurringDaily,
            isRecurringWeekly = isRecurringWeekly ?: this.isRecurringWeekly,
            isRecurringMonthly = isRecurringMonthly ?: this.isRecurringMonthly,
            scheduledStartTimestamp = scheduledStartTimestamp ?: this.scheduledStartTimestamp,
            scheduledEndTimestamp = scheduledEndTimestamp ?: this.scheduledEndTimestamp,
            scheduledTimestampFormatted = scheduledTimestampFormatted
                ?: this.scheduledTimestampFormatted,
            isCancelled = isCancelled ?: this.isCancelled,
            description = description ?: this.description
        )
    }
}
