package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.mapper.mapChatRoomOwnPrivilege
import mega.privacy.android.domain.entity.GroupChatPeer
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.GetMeetingsRepository
import javax.inject.Inject

/**
 * Default implementation of [GetMeetingsRepository]
 *
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property megaApiGateway                     [MegaApiGateway]
 * @property ioDispatcher                       [CoroutineDispatcher]
 */
internal class DefaultGetMeetingsRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetMeetingsRepository {

    override suspend fun getGroupChatPeers(chatId: Long): List<GroupChatPeer> =
        withContext(ioDispatcher) {
            val chatRoom = megaChatApiGateway.getChatRoom(chatId)
                ?: throw ChatRoomDoesNotExistException()

            if (!chatRoom.isGroup || chatRoom.peerCount == 0L) {
                emptyList()
            } else {
                mutableListOf<GroupChatPeer>().apply {
                    for (index in 0..chatRoom.peerCount) {
                        add(
                            GroupChatPeer(
                                chatRoom.getPeerHandle(index),
                                chatRoom.getPeerPrivilege(index).mapChatRoomOwnPrivilege(),
                                chatRoom.getPeerEmail(index),
                                chatRoom.getPeerFullname(index),
                            ))
                    }
                }
            }
        }
}
