package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.ChatPeer
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.ChatRoomPermission
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaHandleList

/**
 * Mapper to convert [MegaChatRequest] to [ChatRequest]
 */
typealias ChatRequestMapper = (@JvmSuppressWildcards MegaChatRequest) -> @JvmSuppressWildcards ChatRequest

internal fun toChatRequest(megaChatRequest: MegaChatRequest): ChatRequest =
    ChatRequest(
        mapChatRequestType(megaChatRequest.type),
        megaChatRequest.requestString,
        megaChatRequest.tag,
        megaChatRequest.number,
        megaChatRequest.numRetry,
        megaChatRequest.flag,
        mapChatRequestPeersList(megaChatRequest.megaChatPeerList),
        megaChatRequest.chatHandle,
        megaChatRequest.userHandle,
        megaChatRequest.privilege,
        megaChatRequest.text,
        megaChatRequest.link,
        mapChatRequestPeersListByChatHandle(megaChatRequest),
        mapChatRequestHandleList(megaChatRequest.megaHandleList),
        mapChatRequestParamType(megaChatRequest.paramType)
    )

private fun mapChatRequestType(chatRequestType: Int): ChatRequestType = when (chatRequestType) {
    MegaChatRequest.TYPE_INITIALIZE -> ChatRequestType.Initialize
    MegaChatRequest.TYPE_CONNECT -> ChatRequestType.Connect
    MegaChatRequest.TYPE_DELETE -> ChatRequestType.Delete
    MegaChatRequest.TYPE_LOGOUT -> ChatRequestType.Logout
    MegaChatRequest.TYPE_SET_ONLINE_STATUS -> ChatRequestType.SetOnlineStatus
    MegaChatRequest.TYPE_START_CHAT_CALL -> ChatRequestType.StartChatCall
    MegaChatRequest.TYPE_ANSWER_CHAT_CALL -> ChatRequestType.AnswerChatCall
    MegaChatRequest.TYPE_DISABLE_AUDIO_VIDEO_CALL -> ChatRequestType.DisableAudioVideoCall
    MegaChatRequest.TYPE_HANG_CHAT_CALL -> ChatRequestType.HangChatCall
    MegaChatRequest.TYPE_CREATE_CHATROOM -> ChatRequestType.CreateChatRoom
    MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM -> ChatRequestType.RemoveFromChatRoom
    MegaChatRequest.TYPE_INVITE_TO_CHATROOM -> ChatRequestType.InviteToChatRoom
    MegaChatRequest.TYPE_UPDATE_PEER_PERMISSIONS -> ChatRequestType.UpdatePeerPermissions
    MegaChatRequest.TYPE_EDIT_CHATROOM_NAME -> ChatRequestType.EditChatRoomName
    MegaChatRequest.TYPE_EDIT_CHATROOM_PIC -> ChatRequestType.EditChatRoomPic
    MegaChatRequest.TYPE_TRUNCATE_HISTORY -> ChatRequestType.TruncateHistory
    MegaChatRequest.TYPE_SHARE_CONTACT -> ChatRequestType.ShareContact
    MegaChatRequest.TYPE_GET_FIRSTNAME -> ChatRequestType.GetFirstName
    MegaChatRequest.TYPE_GET_LASTNAME -> ChatRequestType.GetLastName
    MegaChatRequest.TYPE_DISCONNECT -> ChatRequestType.Disconnect
    MegaChatRequest.TYPE_GET_EMAIL -> ChatRequestType.GetEmail
    MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE -> ChatRequestType.AttachNodeMessage
    MegaChatRequest.TYPE_REVOKE_NODE_MESSAGE -> ChatRequestType.RevokeNodeMessage
    MegaChatRequest.TYPE_SET_BACKGROUND_STATUS -> ChatRequestType.SetBackgroundStatus
    MegaChatRequest.TYPE_RETRY_PENDING_CONNECTIONS -> ChatRequestType.RetryPendingConnections
    MegaChatRequest.TYPE_SEND_TYPING_NOTIF -> ChatRequestType.SendTypingNotification
    MegaChatRequest.TYPE_SIGNAL_ACTIVITY -> ChatRequestType.SignalActivity
    MegaChatRequest.TYPE_SET_PRESENCE_PERSIST -> ChatRequestType.SetPresencePersist
    MegaChatRequest.TYPE_SET_PRESENCE_AUTOAWAY -> ChatRequestType.SetPresenceAutoAway
    MegaChatRequest.TYPE_LOAD_AUDIO_VIDEO_DEVICES -> ChatRequestType.LoadAudioVideoDevices
    MegaChatRequest.TYPE_ARCHIVE_CHATROOM -> ChatRequestType.ArchiveChatRoom
    MegaChatRequest.TYPE_PUSH_RECEIVED -> ChatRequestType.PushReceived
    MegaChatRequest.TYPE_SET_LAST_GREEN_VISIBLE -> ChatRequestType.SetLastGreenVisible
    MegaChatRequest.TYPE_LAST_GREEN -> ChatRequestType.LastGreen
    MegaChatRequest.TYPE_LOAD_PREVIEW -> ChatRequestType.LoadPreview
    MegaChatRequest.TYPE_CHAT_LINK_HANDLE -> ChatRequestType.ChatLinkHandle
    MegaChatRequest.TYPE_SET_PRIVATE_MODE -> ChatRequestType.SetPrivateMode
    MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT -> ChatRequestType.AutoJoinPublicChat
    MegaChatRequest.TYPE_CHANGE_VIDEO_STREAM -> ChatRequestType.ChangeVideoStream
    MegaChatRequest.TYPE_IMPORT_MESSAGES -> ChatRequestType.ImportMessages
    MegaChatRequest.TYPE_SET_RETENTION_TIME -> ChatRequestType.SetRetentionTime
    MegaChatRequest.TYPE_SET_CALL_ON_HOLD -> ChatRequestType.SetCallOnHold
    MegaChatRequest.TYPE_ENABLE_AUDIO_LEVEL_MONITOR -> ChatRequestType.EnableAudioLevelMonitor
    MegaChatRequest.TYPE_MANAGE_REACTION -> ChatRequestType.ManageReaction
    MegaChatRequest.TYPE_GET_PEER_ATTRIBUTES -> ChatRequestType.GetPeerAttributes
    MegaChatRequest.TYPE_REQUEST_SPEAK -> ChatRequestType.RequestSpeak
    MegaChatRequest.TYPE_APPROVE_SPEAK -> ChatRequestType.ApproveSpeak
    MegaChatRequest.TYPE_REQUEST_HIGH_RES_VIDEO -> ChatRequestType.RequestHighResVideo
    MegaChatRequest.TYPE_REQUEST_LOW_RES_VIDEO -> ChatRequestType.RequestLowResVideo
    MegaChatRequest.TYPE_OPEN_VIDEO_DEVICE -> ChatRequestType.OpenVideoDevice
    MegaChatRequest.TYPE_REQUEST_HIRES_QUALITY -> ChatRequestType.RequestHiresQuality
    MegaChatRequest.TYPE_DEL_SPEAKER -> ChatRequestType.DeleteSpeaker
    MegaChatRequest.TYPE_REQUEST_SVC_LAYERS -> ChatRequestType.RequestSVCLayers
    else -> ChatRequestType.InvalidRequest
}

private fun mapChatRequestParamType(chatRequestParamType: Int?): ChatRequestParamType? =
    when (chatRequestParamType) {
        MegaChatRequest.AUDIO -> ChatRequestParamType.Audio
        MegaChatRequest.VIDEO -> ChatRequestParamType.Video
        else -> null
    }

private fun mapChatRequestPeersList(chatRequestPeersList: MegaChatPeerList?): List<ChatPeer>? =
    chatRequestPeersList?.let { peersList ->
        (0 until peersList.size()).map {
            ChatPeer(peersList.getPeerHandle(it), mapPeerPrivilege(peersList.getPeerPrivilege(it)))
        }
    }

private fun mapPeerPrivilege(privilege: Int): ChatRoomPermission = when (privilege) {
    MegaChatRoom.PRIV_RM -> ChatRoomPermission.Removed
    MegaChatRoom.PRIV_RO -> ChatRoomPermission.ReadOnly
    MegaChatRoom.PRIV_STANDARD -> ChatRoomPermission.Standard
    MegaChatRoom.PRIV_MODERATOR -> ChatRoomPermission.Moderator
    else -> ChatRoomPermission.Unknown
}

private fun mapChatRequestPeersListByChatHandle(megaChatRequest: MegaChatRequest): Map<Long, List<Long>>? =
    megaChatRequest.megaHandleList?.let { handleList ->
        (0 until handleList.size())
            .map { handleList[it] }
            .mapNotNull {
                mapChatRequestHandleList(megaChatRequest.getMegaHandleListByChat(it))?.let { list ->
                    Pair(it, list)
                }
            }.toMap()
    }

private fun mapChatRequestHandleList(chatRequestHandleList: MegaHandleList?): List<Long>? =
    chatRequestHandleList?.let { handleList ->
        (0 until handleList.size()).map {
            handleList[it]
        }
    }