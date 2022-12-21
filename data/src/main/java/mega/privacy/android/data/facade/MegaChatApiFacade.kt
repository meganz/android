package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.ChatUpdate
import mega.privacy.android.domain.qualifier.ApplicationScope
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatListenerInterface
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatPeerList
import nz.mega.sdk.MegaChatPresenceConfig
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoomListenerInterface
import javax.inject.Inject

/**
 * Mega chat api facade implementation of the [MegaChatApiGateway]
 *
 * @property chatApi      [MegaChatApiAndroid]
 * @property sharingScope [CoroutineScope]
 */
internal class MegaChatApiFacade @Inject constructor(
    private val chatApi: MegaChatApiAndroid,
    @ApplicationScope private val sharingScope: CoroutineScope,
) : MegaChatApiGateway {

    override val initState: Int
        get() = chatApi.initState

    override fun init(session: String): Int =
        chatApi.init(session)

    override fun logout() = chatApi.logout()

    override fun setLogger(logger: MegaChatLoggerInterface) =
        MegaChatApiAndroid.setLoggerObject(logger)

    override fun setLogLevel(logLevel: Int) = MegaChatApiAndroid.setLogLevel(logLevel)

    override fun addChatRequestListener(listener: MegaChatRequestListenerInterface) =
        chatApi.addChatRequestListener(listener)

    override fun removeChatRequestListener(listener: MegaChatRequestListenerInterface) =
        chatApi.removeChatRequestListener(listener)

    override fun pushReceived(
        beep: Boolean,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.pushReceived(beep, listener)

    override fun retryPendingConnections(
        disconnect: Boolean,
        listener: MegaChatRequestListenerInterface?
    ) = chatApi.retryPendingConnections(disconnect, listener)

    override val chatUpdates: Flow<ChatUpdate>
        get() = callbackFlow {
            val listener = object : MegaChatListenerInterface {
                override fun onChatListItemUpdate(api: MegaChatApiJava?, item: MegaChatListItem?) {
                    trySend(ChatUpdate.OnChatListItemUpdate(item))
                }

                override fun onChatInitStateUpdate(api: MegaChatApiJava?, newState: Int) {
                    trySend(ChatUpdate.OnChatInitStateUpdate(newState))
                }

                override fun onChatOnlineStatusUpdate(
                    api: MegaChatApiJava?,
                    userhandle: Long,
                    status: Int,
                    inProgress: Boolean,
                ) {
                    trySend(ChatUpdate.OnChatOnlineStatusUpdate(userhandle, status, inProgress))
                }

                override fun onChatPresenceConfigUpdate(
                    api: MegaChatApiJava?,
                    config: MegaChatPresenceConfig?,
                ) {
                    trySend(ChatUpdate.OnChatPresenceConfigUpdate(config))
                }

                override fun onChatConnectionStateUpdate(
                    api: MegaChatApiJava?,
                    chatid: Long,
                    newState: Int,
                ) {
                    trySend(ChatUpdate.OnChatConnectionStateUpdate(chatid, newState))
                }

                override fun onChatPresenceLastGreen(
                    api: MegaChatApiJava?,
                    userhandle: Long,
                    lastGreen: Int,
                ) {
                    trySend(ChatUpdate.OnChatPresenceLastGreen(userhandle, lastGreen))
                }

                override fun onDbError(api: MegaChatApiJava?, error: Int, msg: String?) {
                    trySend(ChatUpdate.OnDbError(error, msg))
                }
            }

            chatApi.addChatListener(listener)
            awaitClose { chatApi.removeChatListener(listener) }
        }.shareIn(sharingScope, SharingStarted.WhileSubscribed())

    override fun getChatRoomUpdates(chatId: Long): Flow<ChatRoomUpdate> = callbackFlow {
        val listener = object : MegaChatRoomListenerInterface {
            override fun onChatRoomUpdate(api: MegaChatApiJava?, chat: MegaChatRoom?) {
                trySend(ChatRoomUpdate.OnChatRoomUpdate(chat))
            }

            override fun onMessageLoaded(api: MegaChatApiJava?, msg: MegaChatMessage) {
                trySend(ChatRoomUpdate.OnMessageLoaded(msg))
            }

            override fun onMessageReceived(api: MegaChatApiJava?, msg: MegaChatMessage) {
                trySend(ChatRoomUpdate.OnMessageReceived(msg))
            }

            override fun onMessageUpdate(api: MegaChatApiJava?, msg: MegaChatMessage) {
                trySend(ChatRoomUpdate.OnMessageUpdate(msg))
            }

            override fun onHistoryReloaded(api: MegaChatApiJava?, chat: MegaChatRoom?) {
                trySend(ChatRoomUpdate.OnHistoryReloaded(chat))
            }

            override fun onReactionUpdate(
                api: MegaChatApiJava?,
                msgId: Long,
                reaction: String,
                count: Int,
            ) {
                trySend(ChatRoomUpdate.OnReactionUpdate(msgId, reaction, count))
            }

            override fun onHistoryTruncatedByRetentionTime(
                api: MegaChatApiJava?,
                msg: MegaChatMessage,
            ) {
                trySend(ChatRoomUpdate.OnHistoryTruncatedByRetentionTime(msg))
            }
        }

        chatApi.openChatRoom(chatId, listener)
        awaitClose {
            chatApi.closeChatRoom(chatId, listener)
        }
    }.shareIn(sharingScope, SharingStarted.WhileSubscribed())

    override suspend fun requestLastGreen(userHandle: Long) =
        chatApi.requestLastGreen(userHandle, null)

    override fun createChat(
        isGroup: Boolean,
        peers: MegaChatPeerList,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.createChat(isGroup, peers, listener)

    override fun setOpenInvite(
        chatId: Long,
        enabled: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.setOpenInvite(chatId, enabled, listener)

    override fun getChatRoomByUser(userHandle: Long): MegaChatRoom? =
        chatApi.getChatRoomByUser(userHandle)

    override fun getUserAliasFromCache(userHandle: Long): String? =
        chatApi.getUserAliasFromCache(userHandle)

    override fun getUserFullNameFromCache(userHandle: Long): String? =
        chatApi.getUserFullnameFromCache(userHandle)

    override fun getUserOnlineStatus(userHandle: Long): Int =
        chatApi.getUserOnlineStatus(userHandle)

    override fun getChatRoom(chatId: Long): MegaChatRoom? =
        chatApi.getChatRoom(chatId)


    override fun getChatCall(chatId: Long): MegaChatCall? =
        chatApi.getChatCall(chatId)

    override fun startChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.startChatCall(chatId, enabledVideo, enabledAudio, listener)

    override fun answerChatCall(
        chatId: Long,
        enabledVideo: Boolean,
        enabledAudio: Boolean,
        listener: MegaChatRequestListenerInterface,
    ) = chatApi.answerChatCall(chatId, enabledVideo, enabledAudio, listener)

    override fun setChatVideoInDevice(
        device: String,
        listener: MegaChatRequestListenerInterface?,
    ) = chatApi.setChatVideoInDevice(device, listener)

    companion object {
        const val CHAT_INVALID_HANDLE = MegaChatApiAndroid.MEGACHAT_INVALID_HANDLE
    }
}