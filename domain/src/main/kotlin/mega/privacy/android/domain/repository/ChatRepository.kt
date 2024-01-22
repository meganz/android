package mega.privacy.android.domain.repository

import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.entity.chat.ChatListItem
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatPreview
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomPreference
import mega.privacy.android.domain.entity.chat.CombinedChatRoom
import mega.privacy.android.domain.entity.chat.ConnectionState
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.RichLinkConfig
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.paging.MessagePagingInfo
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.node.NodeId

/**
 * Chat repository
 */
interface ChatRepository {
    /**
     * Get chat init state
     * @return Init state as [ChatInitState]
     */
    suspend fun getChatInitState(): ChatInitState

    /**
     * Init chat API as anonymous user
     * @return Init state as [ChatInitState]
     */
    suspend fun initAnonymousChat(): ChatInitState

    /**
     * Notify chat logout
     *
     * @return a flow that emits true whenever chat api is successfully logged out
     */
    fun notifyChatLogout(): Flow<Boolean>

    /**
     * Gets chat room if it exists
     *
     * @param chatId      Chat Id
     * @return [ChatRoom] containing the updated data.
     */
    suspend fun getChatRoom(chatId: Long): ChatRoom?

    /**
     * Gets chat room if it exists
     *
     * @param userHandle      User handle
     * @return [ChatRoom] containing the updated data.
     */
    suspend fun getChatRoomByUser(userHandle: Long): ChatRoom?

    /**
     * Get chat list item if it exsits
     *
     * @param chatId    Chat Id
     * @return          [ChatListItem]
     */
    suspend fun getChatListItem(chatId: Long): ChatListItem?

    /**
     * Get all chat list items
     *
     * @return  Chat List items
     */
    suspend fun getAllChatListItems(): List<ChatListItem>

    /**
     * Get unread non meeting chat list items
     *
     * @return  Chat List items
     */
    suspend fun getUnreadNonMeetingChatListItems(): List<ChatListItem>

    /**
     * Get unread meeting chat list items
     *
     * @return  Chat List items
     */
    suspend fun getUnreadMeetingChatListItems(): List<ChatListItem>

    /**
     * Update open invite setting.
     *
     * @param chatId   The Chat id.
     * @return True if non-hosts are allowed to add participants, false otherwise.
     */
    suspend fun setOpenInvite(chatId: Long): Boolean

    /**
     * Update open invite setting.
     *
     * @param chatId   The Chat id.
     * @param isOpenInvite True if non-hosts are allowed to add participants, false otherwise.
     */
    suspend fun setOpenInvite(chatId: Long, isOpenInvite: Boolean): ChatRequest

    /**
     * Update waiting room setting.
     *
     * @param chatId    The Chat id.
     * @param enabled   True, should be enabled. False, should be disabled.
     * @return True if non-hosts are allowed to add participants, false otherwise.
     */
    suspend fun setWaitingRoom(chatId: Long, enabled: Boolean): ChatRequest

    /**
     * Leave chat
     *
     * @param chatId    The Chat id.
     */
    suspend fun leaveChat(chatId: Long)

    /**
     * Update chat title.
     *
     * @param chatId    The Chat id.
     * @param title     Title.
     */
    suspend fun setChatTitle(chatId: Long, title: String): ChatRequest

    /**
     * Get chat files folder id if it exists
     */
    suspend fun getChatFilesFolderId(): NodeId?

    /**
     * Get all chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getAllChatRooms(): List<CombinedChatRoom>

    /**
     * Get meeting chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getMeetingChatRooms(): List<CombinedChatRoom>

    /**
     * Get non meeting chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getNonMeetingChatRooms(): List<CombinedChatRoom>

    /**
     * Get Archived chat rooms
     *
     * @return  List of [CombinedChatRoom]
     */
    suspend fun getArchivedChatRooms(): List<CombinedChatRoom>

    /**
     * Gets combined chat room if it exists
     *
     * @param chatId      Chat Id
     * @return [CombinedChatRoom] containing the updated data.
     */
    suspend fun getCombinedChatRoom(chatId: Long): CombinedChatRoom?

    /**
     * Invite contacts to chat.
     *
     * @param chatId            The Chat id.
     * @param contactsData      List of contacts to add
     */
    suspend fun inviteToChat(chatId: Long, contactsData: List<String>)

    /**
     * Invite participant to chat.
     *
     * @param chatId            The Chat id.
     * @param , handle          User handle.
     */
    suspend fun inviteParticipantToChat(chatId: Long, handle: Long): ChatRequest

    /**
     * Set public chat to private.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest].
     */
    suspend fun setPublicChatToPrivate(
        chatId: Long,
    ): ChatRequest

    /**
     * Create chat link.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun createChatLink(chatId: Long): ChatRequest

    /**
     * Remove chat link.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun removeChatLink(chatId: Long): ChatRequest

    /**
     * Allows any user to preview a public chat without being a participant
     *
     * @param link  Public chat link.
     * @return      [ChatRequest]
     */
    suspend fun openChatPreview(link: String): ChatPreview

    /**
     * Obtain basic information abouts a public chat.
     *
     * @param link  Public chat link.
     * @return      [ChatRequest].
     */
    suspend fun checkChatLink(
        link: String,
    ): ChatRequest

    /**
     * Query chat link.
     *
     * @param chatId    The Chat id.
     * @return          [ChatRequest]
     */
    suspend fun queryChatLink(chatId: Long): ChatRequest

    /**
     * Allow a user to add himself to an existing public chat. To do this the public chat must be in preview mode,
     * the result of a previous call to openChatPreview(), and the public handle contained in the chat-link must be still valid.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     */
    suspend fun autojoinPublicChat(chatId: Long)

    /**
     * Allow a user to rejoin to an existing public chat. To do this the public chat
     * must have a valid public handle.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param publicHandle MegaChatHandle that corresponds with the public handle of chat room
     */
    suspend fun autorejoinPublicChat(chatId: Long, publicHandle: Long)

    /**
     * Checks if a Waiting Room chat option is enabled in a bitmask
     *
     * @param chatOptionsBitMask Bitmask that represents a set of chat options
     * @return True if specified option is enabled in the bitmask
     */
    suspend fun hasWaitingRoomChatOptions(chatOptionsBitMask: Int): Boolean

    /**
     * Update chat permissions
     *
     * @param chatId        The chat id.
     * @param handle        User handle.
     * @param permission    User privilege.
     * @return              The Chat Request.
     */
    suspend fun updateChatPermissions(
        chatId: Long,
        handle: Long,
        permission: ChatRoomPermission,
    ): ChatRequest

    /**
     * Remove participant from chat
     *
     * @param chatId    The Chat id.
     * @param handle    User handle
     * @return          [ChatRequest]
     */
    suspend fun removeFromChat(
        chatId: Long,
        handle: Long,
    ): ChatRequest

    /**
     * Invite contact
     *
     * @param email    User email
     * @return
     */
    suspend fun inviteContact(
        email: String,
    ): InviteContactRequest

    /**
     * Monitor updates on chat room item update
     *
     * @param chatId    Chat Id.
     * @return          A flow of [ChatRoom]
     */
    fun monitorChatRoomUpdates(chatId: Long): Flow<ChatRoom>

    /**
     * Load messages from a chat room
     *
     * @param chatId    Chat ID
     * @param count     The number of requested messages to load (Range 1 - 256)
     *
     * @return The source of the messages that is going to be fetched. The possible values are:
     *   - ChatHistoryLoadStatus::ERROR: history has to be fetched from server, but we are not logged in yet
     *   - ChatHistoryLoadStatus::NONE: there's no more history available (not even in the server)
     *   - ChatHistoryLoadStatus::LOCAL: messages will be fetched locally (RAM or DB)
     *   - ChatHistoryLoadStatus::REMOTE: messages will be requested to the server. Expect some delay
     *
     * The value ChatHistoryLoadStatus::REMOTE can be used to show a progress bar accordingly when network operation occurs.
     */
    suspend fun loadMessages(chatId: Long, count: Int): ChatHistoryLoadStatus

    /**
     * Monitor message load on a chat room
     *
     * @param chatId    Chat ID.
     * @return          A flow of [ChatMessage]
     */
    fun monitorOnMessageLoaded(chatId: Long): Flow<ChatMessage?>

    /**
     * Monitor updates on chat list item.
     *
     * @return A flow of [ChatListItem].
     */
    fun monitorChatListItemUpdates(): Flow<ChatListItem>

    /**
     * Returns whether notifications about a chat have to be generated.
     *
     * @param chatId    Chat id
     * @return          True if notifications has to be created, false otherwise.
     */
    suspend fun isChatNotifiable(chatId: Long): Boolean

    /**
     * Check if chat last message geolocation
     *
     * @param chatId    Chat id
     * @return          True if last message is geolocation meta type or false otherwise
     */
    suspend fun isChatLastMessageGeolocation(chatId: Long): Boolean

    /**
     * Get my email updated
     */
    fun monitorMyEmail(): Flow<String?>

    /**
     * Get my name updated
     */
    fun monitorMyName(): Flow<String?>

    /**
     * Reset chat settings if not initialized yet.
     */
    suspend fun resetChatSettings()

    /**
     * Signal chat presence activity
     */
    suspend fun signalPresenceActivity()

    /**
     * Clear chat history
     *
     * @param chatId    Chat id
     */
    suspend fun clearChatHistory(chatId: Long)

    /**
     * Archive chat
     *
     * @param chatId    Chat id
     * @param archive   True to archive, false to unarchive
     */
    suspend fun archiveChat(chatId: Long, archive: Boolean)

    /**
     * Get Peer Handle
     *
     * @param chatId id of the chat
     * @param peerNo required peer number
     * @return peer handle for the selected peer [Long]
     */
    suspend fun getPeerHandle(chatId: Long, peerNo: Long): Long?

    /**
     * Creates a chat if not existing
     *
     * @param isGroup     True if is should create a group chat, false otherwise.
     * @param userHandles List of contact handles.
     * @return The chat conversation handle.
     */
    suspend fun createChat(isGroup: Boolean, userHandles: List<Long>): Long

    /**
     * Creates a groupal chat for one or more participants
     *
     * @param title Null-terminated character string with the chat title. If the title
     * is longer than 30 characters, it will be truncated to that maximum length.
     * @param userHandles List of user handles
     * @param speakRequest True to set that during calls non moderator users, must request permission to speak
     * @param waitingRoom True to set that during calls, non moderator members will be placed into a waiting room.
     * A moderator user must grant each user access to the call.
     * @param openInvite to set that users with MegaChatRoom::PRIV_STANDARD privilege, can invite other users into the chat
     */
    suspend fun createGroupChat(
        title: String?,
        userHandles: List<Long>,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
    ): Long

    /**
     * Creates a public chatroom for multiple participants (groupchat)
     *
     * @param title Null-terminated character string with the chat title. If the title
     * is longer than 30 characters, it will be truncated to that maximum length.
     * @param userHandles List of user handles
     * @param speakRequest True to set that during calls non moderator users, must request permission to speak
     * @param waitingRoom True to set that during calls, non moderator members will be placed into a waiting room.
     * A moderator user must grant each user access to the call.
     * @param openInvite to set that users with MegaChatRoom::PRIV_STANDARD privilege, can invite other users into the chat
     */
    suspend fun createPublicChat(
        title: String?,
        userHandles: List<Long>,
        speakRequest: Boolean,
        waitingRoom: Boolean,
        openInvite: Boolean,
    ): Long

    /**
     * Get user handle given an contact Email
     *
     * @param email Contact email
     * @return      User handle
     */
    suspend fun getContactHandle(email: String): Long?

    /**
     * Returns the current state of the connection
     *
     * @return [ConnectionState]
     */
    suspend fun getConnectionState(): ConnectionState

    /**
     * Returns the current state of the connection to chatId
     *
     * @param chatId
     * @return [ChatConnectionStatus]
     */
    suspend fun getChatConnectionState(chatId: Long): ChatConnectionStatus

    /**
     * Monitor chat archived.
     *
     * @return Flow [String]
     */
    fun monitorChatArchived(): Flow<String>

    /**
     * Broadcast chat archived.
     *
     * @param chatTitle [String]
     */
    suspend fun broadcastChatArchived(chatTitle: String)

    /**
     * Gets the number of unread chats for the logged in user.
     *
     * @return Number of unread chats.
     */
    suspend fun getNumUnreadChats(): Int

    /**
     * Monitor if successfully joined to a chat.
     *
     * @return Flow [Boolean]
     */
    fun monitorJoinedSuccessfully(): Flow<Boolean>

    /**
     * Broadcast if successfully joined to a chat.
     */
    suspend fun broadcastJoinedSuccessfully()

    /**
     * Monitor if should leave a chat.
     *
     * @return Flow [Long] ID of the chat to leave.
     */
    fun monitorLeaveChat(): Flow<Long>

    /**
     * Broadcast that should leave a chat.
     *
     * @param chatId [Long] ID of the chat to leave.
     */
    suspend fun broadcastLeaveChat(chatId: Long)

    /**
     * Returns the [ChatMessage] specified from the chat room.
     *
     * This function allows to retrieve only those messages that are been loaded, received and/or
     * sent (confirmed and not yet confirmed). For any other message, this function
     * will return NULL.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     * @return The [ChatMessage] object, or NULL if not found.
     */
    suspend fun getMessage(chatId: Long, msgId: Long): ChatMessage?

    /**
     * Returns the [ChatMessage] specified from the chat room stored in node history
     *
     * This function allows to retrieve only those messages that are in the node history
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param msgId MegaChatHandle that identifies the message
     * @return The [ChatMessage] object, or NULL if not found.
     */
    suspend fun getMessageFromNodeHistory(chatId: Long, msgId: Long): ChatMessage?

    /**
     * Gets chat message notification behaviour.
     *
     * @param beep Push notification flag indicating if the notification should beep or not.
     * @param defaultSound Default device sound.
     * @return [NotificationBehaviour]
     */
    suspend fun getChatMessageNotificationBehaviour(
        beep: Boolean,
        defaultSound: String?,
    ): NotificationBehaviour

    /**
     * Gets pending messages.
     *
     * @param chatId Chat identifier from which the messages has to be get.
     * @return A list of [PendingMessage].
     */
    suspend fun getPendingMessages(chatId: Long): List<PendingMessage>

    /**
     * Updates a pending message.
     *
     * @param idMessage   Identifier of the pending message.
     * @param transferTag Identifier of the transfer.
     * @param nodeHandle  Handle of the node already uploaded.
     * @param state       State of the pending message.
     */
    suspend fun updatePendingMessage(
        idMessage: Long,
        transferTag: Int,
        nodeHandle: String?,
        state: Int,
    )

    /**
     * Create Ephemeral++ account
     *
     * This kind of account allows to join chat links and to keep the session in the device
     * where it was created.
     *
     * @param firstName Firstname of the user
     * @param lastName Lastname of the user
     * @return Session id to resume the process
     */
    suspend fun createEphemeralAccountPlusPlus(firstName: String, lastName: String): String

    /**
     * Sends a node to the specified chatroom
     *
     * The attachment message includes information about the node, so the receiver can download
     * or import the node.
     *
     * In contrast to other functions to send messages, such as
     * MegaChatApi::sendMessage or MegaChatApi::attachContacts, this function
     * is asynchronous and does not return a MegaChatMessage directly. Instead, the
     * MegaChatMessage can be obtained as a result of the corresponding MegaChatRequest.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_ATTACH_NODE_MESSAGE
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the handle of the node
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getMegaChatMessage - Returns the message that has been sent
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param nodeHandle Handle of the node that the user wants to attach
     * @return Identifier of the temp message attached.
     */
    suspend fun attachNode(chatId: Long, nodeHandle: Long): Long

    /**
     * Sends a node that contains a voice message to the specified chatroom
     *
     * The voice clip message includes information about the node, so the receiver can reproduce it online.
     *
     * In contrast to other functions to send messages, such as MegaChatApi::sendMessage or
     * MegaChatApi::attachContacts, this function is asynchronous and does not return a MegaChatMessage
     * directly. Instead, the MegaChatMessage can be obtained as a result of the corresponding MegaChatRequest.
     *
     * The associated request type with this request is MegaChatRequest::TYPE_ATTACH_NODE_MESSAGE
     * Valid data in the MegaChatRequest object received on callbacks:
     * - MegaChatRequest::getChatHandle - Returns the chat identifier
     * - MegaChatRequest::getUserHandle - Returns the handle of the node
     * - MegaChatRequest::getParamType - Returns 1 (to identify the attachment as a voice message)
     *
     * Valid data in the MegaChatRequest object received in onRequestFinish when the error code
     * is MegaError::ERROR_OK:
     * - MegaChatRequest::getMegaChatMessage - Returns the message that has been sent
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param nodeHandle Handle of the node that the user wants to attach
     * @return Tdentifier of the temp message attached.
     */
    suspend fun attachVoiceMessage(chatId: Long, nodeHandle: Long): Long

    /**
     * Get own privilege
     *
     * @param chatId    Chat id
     * @return          [ChatRoomPermission]
     */
    suspend fun getOwnPrivilege(chatId: Long): ChatRoomPermission

    /**
     * Get user privilege
     *
     * @param chatId        Chat id
     * @param userHandle    User handle
     * @return              [ChatRoomPermission]
     */
    suspend fun getUserPrivilege(chatId: Long, userHandle: Long): ChatRoomPermission

    /**
     * Get chat invalid handle
     * it returns MEGACHAT_INVALID_HANDLE so don't need to be suspend
     */
    fun getChatInvalidHandle(): Long

    /**
     * Returns true if there is a call at chatroom with id chatId.
     * Not it's not necessary that we participate in the call, but other participants do.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @return True if there is a call in a chatroom. False in other case
     */
    suspend fun hasCallInChatRoom(chatId: Long): Boolean

    /**
     * Get participant first name
     *
     * @param handle User handle.
     * @param contemplateEmail True if should return the email if the name is not found, false otherwise.
     * @return First name of a chat participant or the email if not found and contemplated.
     */
    suspend fun getParticipantFirstName(handle: Long, contemplateEmail: Boolean): String?

    /**
     * Get participant full name
     *
     * @param handle
     * @return full name of a chat participant
     */
    suspend fun getParticipantFullName(handle: Long): String?

    /**
     * Get my user handle
     *
     * @return my user handle
     */
    suspend fun getMyUserHandle(): Long

    /**
     * @brief Returns if audio level monitor is enabled
     *
     * It's false by default
     *
     * @note If there isn't a call in that chatroom in which user is participating,
     * audio Level monitor will be always false
     *
     * @param chatId MegaChatHandle that identifies the chat room from we want know if audio level monitor is disabled
     * @return true if audio level monitor is enabled
     */
    suspend fun isAudioLevelMonitorEnabled(chatId: Long): Boolean

    /**
     * Enable or disable audio level monitor.
     *
     * Audio level monitor detects when a peer starts or stops speaking, and triggers a callback
     * (onChatSessionUpdate with change type CHANGE_TYPE_AUDIO_LEVEL) to inform apps about that event.
     *
     * It's false by default and it's app responsibility to enable it
     *
     * @param enable True for enable audio level monitor, False to disable
     * @param chatId MegaChatHandle that identifies the chat room where we can enable audio level monitor
     */
    suspend fun enableAudioLevelMonitor(enable: Boolean, chatId: Long)

    /**
     * End chat call
     *
     * @param chatId   Call id
     * @return True if the call was ended, false otherwise.
     */
    suspend fun endChatCall(chatId: Long): Boolean

    /**
     * Check if the sending of geolocation messages is enabled
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_GEOLOCATION
     * <p>
     * Sending a Geolocation message is enabled if the MegaRequest object, received in onRequestFinish,
     * has error code MegaError::API_OK. In other cases, send geolocation messages is not enabled and
     * the application has to answer before send a message of this type.
     */
    suspend fun isGeolocationEnabled(): Boolean

    /**
     * Enable the sending of geolocation messages
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the attribute type MegaApi::USER_ATTR_GEOLOCATION

     */
    suspend fun enableGeolocation()

    /**
     * Get my full name
     *
     * @return my full name
     */
    suspend fun getMyFullName(): String?

    /**
     * Sends a new message to the specified chatroom
     *
     * The ChatMessage object returned by this function includes a message transaction id,
     * That id is not the definitive id, which will be assigned by the server. You can obtain the
     * temporal id with ChatMessage.tempId
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to INVALID_HANDLE.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param message Content of the message
     * application-specific type like link, share, picture etc.) @see MegaChatMessage::Type.
     *
     * @return ChatMessage that will be sent. The message id is not definitive, but temporal.
     */
    suspend fun sendMessage(chatId: Long, message: String): ChatMessage

    /**
     * Set last public handle
     *
     * @param handle
     */
    suspend fun setLastPublicHandle(handle: Long)

    /**
     * Close chat preview
     *
     * @param chatId
     */
    suspend fun closeChatPreview(chatId: Long)

    /**
     * Should show rich link warning
     *
     * @return true if should show rich link warning, false otherwise
     */
    suspend fun shouldShowRichLinkWarning(): Boolean

    /**
     * Is rich link enabled
     *
     * @return true if rich link is enabled, false otherwise
     */
    suspend fun isRichPreviewsEnabled(): Boolean

    /**
     * Monitor rich link preview config
     *
     * @return Flow [RichLinkConfig]
     */
    fun monitorRichLinkPreviewConfig(): Flow<RichLinkConfig>

    /**
     * Set rich link warning counter value
     *
     * @param value
     * @return number of rich link warning counter
     */
    suspend fun setRichLinkWarningCounterValue(value: Int): Int

    /**
     * Has url
     *
     * @param url
     * @return true if has url, false otherwise
     */
    fun hasUrl(url: String): Boolean

    /**
     * Enable rich link preview
     *
     * @param enable
     */
    suspend fun enableRichPreviews(enable: Boolean)

    /**
     * Get paged messages
     *
     * @param chatId
     * @return flow of paged messages
     */
    fun getPagedMessages(chatId: Long): PagingSource<Int, TypedMessage>

    /**
     * Store messages
     *
     * @param chatId
     * @param messages
     */
    suspend fun storeMessages(chatId: Long, messages: List<CreateTypedMessageRequest>)

    /**
     * Get last load response
     *
     * @param chatId
     * @return last history load status for the chat
     */
    suspend fun getLastLoadResponse(chatId: Long): ChatHistoryLoadStatus?

    /**
     * Set last load response
     *
     * @param chatId
     * @param status
     */
    suspend fun setLastLoadResponse(chatId: Long, status: ChatHistoryLoadStatus)

    /**
     * Clear chat messages
     *
     * @param chatId
     */
    suspend fun clearChatMessages(chatId: Long)

    /**
     * Get next message
     *
     * @param chatId
     * @param timestamp
     * @return next chronological message if it exists
     */
    suspend fun getNextMessagePagingInfo(chatId: Long, timestamp: Long): MessagePagingInfo?

    /**
     * Returns the folder for saving chat files in user attributes, null if it's not configured yet
     */
    suspend fun getMyChatsFilesFolderId(): NodeId

    /**
     * Monitor if chat is in joining state.
     */
    fun monitorJoiningChat(chatId: Long): Flow<Boolean>

    /**
     * Monitor if chat is in leaving state.
     */
    fun monitorLeavingChat(chatId: Long): Flow<Boolean>

    /**
     * Share a geolocation in the specified chatroom
     *
     * The MegaChatMessage object returned by this function includes a message transaction id,
     * That id is not the definitive id, which will be assigned by the server. You can obtain the
     * temporal id with MegaChatMessage::getTempId
     *
     * When the server confirms the reception of the message, the MegaChatRoomListener::onMessageUpdate
     * is called, including the definitive id and the new status: MegaChatMessage::STATUS_SERVER_RECEIVED.
     * At this point, the app should refresh the message identified by the temporal id and move it to
     * the final position in the history, based on the reported index in the callback.
     *
     * If the message is rejected by the server, the message will keep its temporal id and will have its
     * a message id set to MEGACHAT_INVALID_HANDLE.
     *
     * You take the ownership of the returned value.
     *
     * @param chatId MegaChatHandle that identifies the chat room
     * @param longitude from shared geolocation
     * @param latitude from shared geolocation
     * @param image Preview as a byte array encoded in Base64URL. It can be NULL
     * @return [ChatMessage] that will be sent. The message id is not definitive, but temporal.
     */
    suspend fun sendGeolocation(
        chatId: Long,
        longitude: Float,
        latitude: Float,
        image: String,
    ): ChatMessage

    /**
     * Set chat room preference
     *
     * @param chatId   Chat id
     * @param draftMessage Draft message
     */
    suspend fun setChatDraftMessage(chatId: Long, draftMessage: String)

    /**
     * Get chat room preference
     *
     * @param chatId   Chat id
     * @return         Chat room preference
     */
    fun getChatRoomPreference(chatId: Long): Flow<ChatRoomPreference>
}
