package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.data.mapper.transfer.TransferQuotaStateMapper
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.exception.FetchChatMegaNodeException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import java.io.File
import javax.inject.Inject

/**
 * Mapper from mega node to method to get FullSize Image from server
 */
internal class FullImageFromServerMapper @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaNodeFromChatMessageMapper: MegaNodeFromChatMessageMapper,
    private val transferQuotaStateMapper: TransferQuotaStateMapper,
    private val appEventGateway: AppEventGateway,
    @ApplicationScope private val appScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Mapper from mega node to method to get FullSize Image from server
     * Should only be used for non-chat images
     * @param megaNode [MegaNode]
     * @return a suspend block to be executed to get FullSize Image from server
     */
    operator fun invoke(
        megaNode: MegaNode,
    ) = { path: String, highPriority: Boolean, resetDownloads: () -> Unit ->
        callbackFlow {
            val listener = fullImageListener(path, resetDownloads)

            megaApiGateway.getFullImage(
                megaNode,
                File(path),
                highPriority, listener
            )

            awaitClose()
        }.flowOn(ioDispatcher)
    }

    /**
     * Mapper from chat message to method to get FullSize Image from server
     * This version delays fetching the MegaNode until download is actually needed,
     * Avoiding memory issues where GC-released chat messages cause MegaNode to become a dangling pointer.
     * @param chatId Chat room ID
     * @param messageId Message ID
     * @return a suspend block to be executed to get FullSize Image from server
     */
    operator fun invoke(
        chatId: Long,
        messageId: Long,
    ) = { path: String, highPriority: Boolean, resetDownloads: () -> Unit ->
        callbackFlow {
            val listener = fullImageListener(path, resetDownloads)

            // Re-fetch the MegaNode when download is actually needed
            val megaNode = megaNodeFromChatMessageMapper(chatId, messageId)
                ?: run {
                    cancel(
                        "Chat node not found", FetchChatMegaNodeException(
                            chatId = chatId,
                            messageId = messageId
                        )
                    )
                    return@callbackFlow
                }

            megaApiGateway.getFullImage(
                megaNode,
                File(path),
                highPriority, listener
            )

            awaitClose()
        }.flowOn(ioDispatcher)
    }

    private fun ProducerScope<ImageProgress>.fullImageListener(
        path: String,
        resetDownloads: () -> Unit,
    ) = OptionalMegaTransferListenerInterface(
        onTransferStart = { transfer ->
            trySend(ImageProgress.Started(transfer.tag))
        },
        onTransferFinish = { _: MegaTransfer, error: MegaError ->
            when (error.errorCode) {
                MegaError.API_OK, MegaError.API_EEXIST, MegaError.API_ENOENT -> {
                    trySend(ImageProgress.Completed(path))
                }

                else -> {
                    cancel(error.errorString, error.toException("getFullImage"))
                }
            }
            resetDownloads()
            close()
        },
        onTransferTemporaryError = { _, error ->
            if (error.errorCode == MegaError.API_EOVERQUOTA) {
                cancel(error.errorString, error.toException("getFullImage"))
            }
        },
        onTransferUpdate = {
            trySend(ImageProgress.InProgress(it.totalBytes, it.transferredBytes))
        },
        // Broadcast on the app scope rather than this flow's: the state is reported before the
        // image starts downloading, so swiping to the next one would cancel the warning with it.
        onTransferQuotaState = { _, state ->
            appScope.launch {
                appEventGateway.broadcastTransferQuotaState(transferQuotaStateMapper(state))
            }
        },
    )
}
