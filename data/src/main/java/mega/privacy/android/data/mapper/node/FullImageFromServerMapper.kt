package mega.privacy.android.data.mapper.node

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.domain.entity.imageviewer.ImageProgress
import mega.privacy.android.domain.exception.MegaException
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
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Mapper from mega node to method to get FullSize Image from server
     * @param megaNode [MegaNode]
     * @return a suspend block to be executed to get FullSize Image from server
     */
    operator fun invoke(
        megaNode: MegaNode,
    ) = { path: String, highPriority: Boolean, resetDownloads: () -> Unit ->
        callbackFlow {
            val listener = OptionalMegaTransferListenerInterface(
                onTransferStart = { transfer ->
                    trySend(ImageProgress.Started(transfer.tag))
                },
                onTransferFinish = { _: MegaTransfer, error: MegaError ->
                    when (error.errorCode) {
                        MegaError.API_OK -> {
                            trySend(ImageProgress.Completed(path))
                        }

                        MegaError.API_EEXIST -> {
                            trySend(ImageProgress.Completed(path))
                        }

                        MegaError.API_ENOENT -> {
                            trySend(ImageProgress.Completed(path))
                        }

                        else -> {
                            cancel(
                                error.errorString, MegaException(
                                    error.errorCode,
                                    error.errorString
                                )
                            )
                        }
                    }
                    resetDownloads()
                    close()
                },
                onTransferTemporaryError = { _, error ->
                    if (error.errorCode == MegaError.API_EOVERQUOTA) {
                        cancel(
                            error.errorString, MegaException(
                                error.errorCode,
                                error.errorString
                            )
                        )
                    }
                },
                onTransferUpdate = {
                    trySend(ImageProgress.InProgress(it.totalBytes, it.transferredBytes))
                }
            )

            megaApiGateway.getFullImage(
                megaNode,
                File(path),
                highPriority, listener
            )

            awaitClose {
                megaApiGateway.removeTransferListener(listener)
            }
        }.flowOn(ioDispatcher)
    }
}
