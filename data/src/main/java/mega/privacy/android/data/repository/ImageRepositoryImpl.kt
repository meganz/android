package mega.privacy.android.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithException
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.ImageRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import javax.inject.Inject
import kotlin.coroutines.resumeWithException

/**
 * The repository implementation class regarding thumbnail feature.
 *
 * @param context Context
 * @param megaApiGateway MegaApiGateway
 * @param megaChatApiGateway MegaChatApiGateway
 * @param ioDispatcher CoroutineDispatcher
 * @param cacheGateway CacheGateway
 * @param fileManagementPreferencesGateway FileManagementPreferencesGateway
 * @param fileGateway FileGateway
 * @param imageNodeMapper ImageNodeMapper
 */
internal class ImageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val megaApiGateway: MegaApiGateway,
    private val megaApiFolderGateway: MegaApiFolderGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val cacheGateway: CacheGateway,
    private val fileManagementPreferencesGateway: FileManagementPreferencesGateway,
    private val fileGateway: FileGateway,
    private val imageNodeMapper: ImageNodeMapper,
    private val megaLocalRoomGateway: MegaLocalRoomGateway
) : ImageRepository {
    override suspend fun getImageNodeByHandle(handle: Long): ImageNode =
        withContext(ioDispatcher) {
            getImageNode {
                megaApiGateway.getMegaNodeByHandle(handle)
                    ?: megaApiFolderGateway.getMegaNodeByHandle(handle)
                        ?.let { megaApiFolderGateway.authorizeNode(it) }
            }
        }

    override suspend fun getImageNodeForPublicLink(nodeFileLink: String): ImageNode =
        withContext(ioDispatcher) {
            getImageNode { getPublicMegaNode(nodeFileLink) }
        }

    override suspend fun getImageNodeForChatMessage(
        chatRoomId: Long,
        chatMessageId: Long,
    ): ImageNode =
        withContext(ioDispatcher) {
            getImageNode { getChatMegaNode(chatRoomId, chatMessageId) }
        }

    private suspend fun getImageNode(getMegaNode: suspend () -> MegaNode?): ImageNode =
        withContext(ioDispatcher) {
            val megaNode = getMegaNode()
                ?: throw IllegalArgumentException("Node not found")
            val offline = megaLocalRoomGateway.getOfflineInformation(megaNode.handle)
            imageNodeMapper(
                megaNode = megaNode, hasVersion = megaApiGateway::hasVersion, offline = offline
            )
        }

    private suspend fun getPublicMegaNode(nodeFileLink: String) =
        suspendCancellableCoroutine { continuation ->
            val listener = OptionalMegaRequestListenerInterface(
                onRequestFinish = { request, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        if (!request.flag) {
                            continuation.resumeWith(Result.success(request.publicMegaNode))
                        } else {
                            continuation.resumeWithException(IllegalArgumentException("Invalid key for public node"))
                        }
                    } else {
                        continuation.failWithException(error.toException("getPublicMegaNode"))
                    }
                }
            )
            megaApiGateway.getPublicNode(nodeFileLink, listener)
            continuation.invokeOnCancellation {
                megaApiGateway.removeRequestListener(listener)
            }
        }

    private fun getChatMegaNode(chatRoomId: Long, chatMessageId: Long): MegaNode? {
        val chatMessage = megaChatApiGateway.getMessage(chatRoomId, chatMessageId)
            ?: megaChatApiGateway.getMessageFromNodeHistory(chatRoomId, chatMessageId)

        val megaNode = chatMessage?.let {
            val node = chatMessage.megaNodeList.get(0)
            val chatRoom = megaChatApiGateway.getChatRoom(chatRoomId)

            if (chatRoom?.isPreview == true) {
                megaApiGateway.authorizeChatNode(node, chatRoom.authorizationToken) ?: node
            } else {
                node
            }
        }
        return megaNode
    }
}
