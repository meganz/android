package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.toException
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaTransferListenerInterface
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.SupportRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Default support repository implementation
 *
 * @property megaApi
 */
internal class DefaultSupportRepository @Inject constructor(
    private val megaApi: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SupportRepository {
    override suspend fun logTicket(ticketContent: String) =
        withContext(ioDispatcher) {
            Timber.d("SupportRepository: logTicket called")
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { _, error ->
                        if (error.errorCode == MegaError.API_OK) {
                            Timber.d("SupportRepository: logTicket success")
                            continuation.resumeWith(Result.success(Unit))
                        } else {
                            continuation.failWithError(error, "logTicket")
                        }
                    }
                )
                megaApi.createSupportTicket(ticketContent, listener)
            }
        }

    override fun uploadFile(file: File): Flow<Float> = callbackFlow {
        Timber.d("SupportRepository: uploadFile called")
        var transfer: MegaTransfer? = null
        val callback = uploadFileInterface(channel) { transfer = it }

        megaApi.startUploadForSupport(file.path, callback)

        awaitClose {
            transfer?.let { megaApi.cancelTransfer(transfer = it, listener = null) }
        }
    }.flowOn(ioDispatcher)

    private fun uploadFileInterface(
        channel: SendChannel<Float>,
        onStart: (MegaTransfer) -> Unit,
    ) = OptionalMegaTransferListenerInterface(
        onTransferUpdate = onUploadFileUpdate(channel),
        onTransferFinish = onUploadFileFinish(channel),
        onTransferTemporaryError = onUploadError(),
        onTransferStart = onStart
    )

    private fun onUploadFileUpdate(channel: SendChannel<Float>): (MegaTransfer) -> Unit =
        { transfer ->
            val fractionUploaded =
                transfer.transferredBytes.toFloat() / transfer.totalBytes.toFloat()
            channel.trySend(fractionUploaded)
        }

    private fun onUploadFileFinish(channel: SendChannel<Float>): (MegaTransfer, MegaError) -> Unit =
        { _, error ->
            val exception =
                error.takeUnless { it.errorCode == MegaError.API_OK }
                    ?.toException("onUploadFileFinish")
            channel.close(exception)
        }

    private fun onUploadError(): (MegaTransfer, MegaError) -> Unit =
        { _, error ->
            Timber.w(
                error.toException("onUploadError"),
                "A temporary error occurred whilst uploading log files to support"
            )
        }

    override suspend fun getSupportEmail() = "support@mega.io"
}
