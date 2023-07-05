package mega.privacy.android.domain.entity

/**
 * Chat request.
 *
 * @property type                  Type of request.
 * @property requestString         A readable string that shows the type of request.
 * @property tag                   Tag that identifies this request.
 * @property number                Number related to the request.
 * @property numRetry              Number of times that a request has temporarily failed.
 * @property flag                  Flag related to the request.
 *                                 This value is valid for these requests:
 *                                 - [ChatRequestType.CreateChatRoom]: Creates a chat for one or more participants.
 *                                 - [ChatRequestType.LoadPreview]: True if it's a meeting room.
 * @property peersList             List of [ChatPeer] in a chat.
 *                                 This value is valid for [ChatRequestType.CreateChatRoom].
 * @property chatHandle            Handle identifying the chat.
 * @property userHandle            Handle identifying the user.
 * @property privilege             Privilege level.
 * @property text                  Text relative to this request.
 * @property link                  Link relative to this request.
 * @property peersListByChatHandle Map of chat handles with their peers list related to the request.
 *                                 This value is valid for [ChatRequestType.PushReceivedUseCase].
 *                                 Each key is a chat handle, and its value corresponds to a list of
 *                                 user handles for unread messages in that chat.
 *                                 You can get the chat handles list with [handleList]
 * @property handleList            List of handles related to this request.
 *                                 This value is valid for these requests:
 *                                 - [ChatRequestType.PushReceivedUseCase]: List of chat handles with unread messages.
 *                                 - [ChatRequestType.LoadPreview] : List with one call identifier, if call doesn't exit it will be NULL.
 * @property paramType             Type of parameter related to the request.
 *                                 This value is valid for these requests:
 *                                 - [ChatRequestType.DisableAudioVideoCall]: ChatRequestParamType.Audio if audio request, ChatRequestParamType.Video if video request.
 *                                 - [ChatRequestType.AttachNodeMessage]: One for voice clips, zero for other nodes.
 *                                 - [ChatRequestType.RetryPendingConnections]: One for refreshUrl
 *                                 - [ChatRequestType.PushReceivedUseCase]: Zero
 */
data class ChatRequest(
    val type: ChatRequestType,
    val requestString: String?,
    val tag: Int,
    val number: Long,
    val numRetry: Int,
    val flag: Boolean,
    val peersList: List<ChatPeer>?,
    val chatHandle: Long?,
    val userHandle: Long?,
    val privilege: Int?,
    val text: String?,
    val link: String?,
    val peersListByChatHandle: Map<Long, List<Long>>?,
    val handleList: List<Long>?,
    val paramType: ChatRequestParamType?,
)