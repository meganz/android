package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.contacts.UserStatus

/**
 * Chat room item
 *
 * @property chatId
 * @property title
 * @property lastMessage
 * @property isLastMessageVoiceClip
 * @property isLastMessageGeolocation
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
 * @constructor Create empty chat room item
 */
sealed class ChatRoomItem(
    open val chatId: Long,
    open val title: String,
    open val lastMessage: String? = null,
    open val isLastMessageVoiceClip: Boolean = false,
    open val isLastMessageGeolocation: Boolean = false,
    open val currentCallStatus: ChatRoomItemStatus? = null,
    open val unreadCount: Int = 0,
    open val hasPermissions: Boolean = false,
    open val isActive: Boolean = false,
    open val isMuted: Boolean = false,
    open val isArchived: Boolean = false,
    open val lastTimestamp: Long = 0L,
    open val lastTimestampFormatted: String? = null,
    open val highlight: Boolean = false,
    open val header: String? = null,
) {

    /**
     * Individual chat room item
     *
     * @property userStatus
     * @property avatar
     * @property peerHandle
     * @property peerEmail
     * @property chatId
     * @property title
     * @property lastMessage
     * @property isLastMessageVoiceClip
     * @property isLastMessageGeolocation
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
        val userStatus: UserStatus? = null,
        val avatar: ChatAvatarItem? = null,
        val peerHandle: Long? = null,
        val peerEmail: String? = null,
        override val chatId: Long,
        override val title: String,
        override val lastMessage: String? = null,
        override val isLastMessageVoiceClip: Boolean = false,
        override val isLastMessageGeolocation: Boolean = false,
        override val currentCallStatus: ChatRoomItemStatus? = null,
        override val unreadCount: Int = 0,
        override val hasPermissions: Boolean = false,
        override val isActive: Boolean = false,
        override val isMuted: Boolean = false,
        override val isArchived: Boolean = false,
        override val lastTimestamp: Long = 0L,
        override val lastTimestampFormatted: String? = null,
        override val highlight: Boolean = false,
        override val header: String? = null,
    ) : ChatRoomItem(
        chatId, title, lastMessage, isLastMessageVoiceClip, isLastMessageGeolocation,
        currentCallStatus, unreadCount, hasPermissions, isActive, isMuted, isArchived,
        lastTimestamp, lastTimestampFormatted, highlight, header
    )

    /**
     * Group chat room item
     *
     * @property isPublic
     * @property avatars
     * @property chatId
     * @property title
     * @property lastMessage
     * @property isLastMessageVoiceClip
     * @property isLastMessageGeolocation
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
        override val chatId: Long,
        override val title: String,
        override val lastMessage: String? = null,
        override val isLastMessageVoiceClip: Boolean = false,
        override val isLastMessageGeolocation: Boolean = false,
        override val currentCallStatus: ChatRoomItemStatus? = null,
        override val unreadCount: Int = 0,
        override val hasPermissions: Boolean = false,
        override val isActive: Boolean = false,
        override val isMuted: Boolean = false,
        override val isArchived: Boolean = false,
        override val lastTimestamp: Long = 0L,
        override val lastTimestampFormatted: String? = null,
        override val highlight: Boolean = false,
        override val header: String? = null,
    ) : ChatRoomItem(
        chatId, title, lastMessage, isLastMessageVoiceClip, isLastMessageGeolocation,
        currentCallStatus, unreadCount, hasPermissions, isActive, isMuted, isArchived,
        lastTimestamp, lastTimestampFormatted, highlight, header
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
     * @property isPublic
     * @property avatars
     * @property chatId
     * @property title
     * @property lastMessage
     * @property isLastMessageVoiceClip
     * @property isLastMessageGeolocation
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
        val isPublic: Boolean = false,
        val avatars: List<ChatAvatarItem>? = null,
        override val chatId: Long,
        override val title: String,
        override val lastMessage: String? = null,
        override val isLastMessageVoiceClip: Boolean = false,
        override val isLastMessageGeolocation: Boolean = false,
        override val currentCallStatus: ChatRoomItemStatus? = null,
        override val unreadCount: Int = 0,
        override val hasPermissions: Boolean = false,
        override val isActive: Boolean = false,
        override val isMuted: Boolean = false,
        override val isArchived: Boolean = false,
        override val lastTimestamp: Long = 0L,
        override val lastTimestampFormatted: String? = null,
        override val highlight: Boolean = false,
        override val header: String? = null,
    ) : ChatRoomItem(
        chatId, title, lastMessage, isLastMessageVoiceClip, isLastMessageGeolocation,
        currentCallStatus, unreadCount, hasPermissions, isActive, isMuted, isArchived,
        lastTimestamp, lastTimestampFormatted, highlight, header
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
        this is MeetingChatRoomItem && this.isPending

    /**
     * Check if chat is a recurring meeting
     *
     * @return  True if is a recurring meeting, false otherwise.
     */
    fun isRecurringMeeting(): Boolean =
        this is MeetingChatRoomItem && this.isRecurring()

    /**
     * Check if chat has ongoing call
     *
     * @return  True if has an ongoing call, false otherwise.
     */
    fun hasOngoingCall(): Boolean =
        currentCallStatus != null && currentCallStatus !is ChatRoomItemStatus.NotStarted

    /**
     * Get chat room item avatars
     *
     * @return  List of [ChatAvatarItem]
     */
    fun getChatAvatars(): List<ChatAvatarItem>? = when (this) {
        is IndividualChatRoomItem -> avatar?.let(::listOf)
        is GroupChatRoomItem -> avatars
        is MeetingChatRoomItem -> avatars
    }

    /**
     * Returns a copy of this chat room item with the provided parameters.
     */
    fun copyChatRoomItem(
        chatId: Long = this.chatId,
        title: String = this.title,
        lastMessage: String? = this.lastMessage,
        isLastMessageVoiceClip: Boolean = this.isLastMessageVoiceClip,
        isLastMessageGeolocation: Boolean = this.isLastMessageGeolocation,
        currentCall: ChatRoomItemStatus? = this.currentCallStatus,
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
        userStatus: UserStatus? = null,
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
    ): ChatRoomItem = when (this) {
        is IndividualChatRoomItem -> copy(
            chatId = chatId,
            title = title,
            lastMessage = lastMessage,
            isLastMessageVoiceClip = isLastMessageVoiceClip,
            isLastMessageGeolocation = isLastMessageGeolocation,
            currentCallStatus = currentCall,
            unreadCount = unreadCount,
            hasPermissions = hasPermissions,
            isActive = isActive,
            isArchived = isArchived,
            isMuted = isMuted,
            lastTimestamp = lastTimestamp,
            lastTimestampFormatted = lastTimestampFormatted,
            highlight = highlight,
            header = header,
            userStatus = userStatus ?: this.userStatus,
            avatar = avatarItems?.firstOrNull() ?: this.avatar,
            peerHandle = peerHandle ?: this.peerHandle,
            peerEmail = peerEmail ?: this.peerEmail,
        )

        is GroupChatRoomItem -> copy(
            chatId = chatId,
            title = title,
            lastMessage = lastMessage,
            isLastMessageVoiceClip = isLastMessageVoiceClip,
            isLastMessageGeolocation = isLastMessageGeolocation,
            currentCallStatus = currentCall,
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
        )

        is MeetingChatRoomItem -> copy(
            chatId = chatId,
            title = title,
            lastMessage = lastMessage,
            isLastMessageVoiceClip = isLastMessageVoiceClip,
            isLastMessageGeolocation = isLastMessageGeolocation,
            currentCallStatus = currentCall,
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
            schedId = schedId ?: this.schedId,
            isPending = isPending ?: this.isPending,
            isRecurringDaily = isRecurringDaily ?: this.isRecurringDaily,
            isRecurringWeekly = isRecurringWeekly ?: this.isRecurringWeekly,
            isRecurringMonthly = isRecurringMonthly ?: this.isRecurringMonthly,
            scheduledStartTimestamp = scheduledStartTimestamp ?: this.scheduledStartTimestamp,
            scheduledEndTimestamp = scheduledEndTimestamp ?: this.scheduledEndTimestamp,
            scheduledTimestampFormatted = scheduledTimestampFormatted
                ?: this.scheduledTimestampFormatted,
        )
    }
}
