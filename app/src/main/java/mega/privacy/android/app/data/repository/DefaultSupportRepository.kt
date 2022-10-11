package mega.privacy.android.app.data.repository

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import mega.privacy.android.app.data.extensions.failWithError
import mega.privacy.android.app.data.extensions.toException
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.listeners.OptionalMegaTransferListenerInterface
import mega.privacy.android.app.utils.Constants.MAIL_SUPPORT
import mega.privacy.android.domain.repository.SupportRepository
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

/**
 * Default support repository implementation
 *
 * @property megaApi
 */
class DefaultSupportRepository @Inject constructor(
    private val megaApi: MegaApiGateway,
) : SupportRepository {
    override suspend fun logTicket(ticketContent: String) = suspendCoroutine<Unit> { continuation ->
        megaApi.createSupportTicket(ticketContent,
            OptionalMegaRequestListenerInterface(
                onRequestFinish = { _, error ->
                    if (error.errorCode == MegaError.API_OK) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.failWithError(error)
                    }
                }
            ))
    }

    override fun uploadFile(file: File): Flow<Float> = callbackFlow {
        var transfer: MegaTransfer? = null
        val callback = uploadFileInterface(channel) { transfer = it }

        megaApi.startUploadForSupport(file.path, callback)

        awaitClose {
            transfer?.let { megaApi.cancelTransfer(it) }
        }
    }

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
                error.takeUnless { it.errorCode == MegaError.API_OK }?.toException()
            channel.close(exception)
        }

    private fun onUploadError(): (MegaTransfer, MegaError) -> Unit =
        { _, error ->
            Timber.w(error.toException(),
                "A temporary error occurred whilst uploading log files to support")
        }

    override suspend fun getSupportEmail() = MAIL_SUPPORT
}
