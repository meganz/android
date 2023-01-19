package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.ScannedContactLinkResultMapper
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.QRCodeRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import java.io.File
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts

/**
 * Implementation of [QRCodeRepository]
 */
@ExperimentalContracts
class DefaultQRCodeRepository @Inject constructor(
    private val cacheFolderGateway: CacheFolderGateway,
    private val megaApiGateway: MegaApiGateway,
    private val megaLocalStorageGateway: MegaLocalStorageGateway,
    private val scannedContactLinkResultMapper: ScannedContactLinkResultMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : QRCodeRepository {

    override suspend fun getQRFile(fileName: String): File? = withContext(defaultDispatcher) {
        cacheFolderGateway.getCacheFile(CacheFolderConstant.QR_FOLDER, fileName)
    }

    override suspend fun queryScannedContactLink(scannedHandle: String) =
        withContext(ioDispatcher) {
            val request = suspendCancellableCoroutine { continuation ->
                val handle = megaApiGateway.base64ToHandle(scannedHandle.trim { it <= ' ' })
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request: MegaRequest, error: MegaError ->
                        continuation.resumeWith(Result.success(request to error))
                    }
                )
                megaApiGateway.getContactLink(handle, listener)
                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }

            scannedContactLinkResultMapper(
                request.first,
                request.second,
                queryIfIsContact(request.first.email)
            )
        }

    override suspend fun updateDatabaseOnQueryScannedContactSuccess(nodeHandle: Long) =
        withContext(ioDispatcher) {
            megaLocalStorageGateway.setLastPublicHandle(nodeHandle)
            megaLocalStorageGateway.setLastPublicHandleTimeStamp()
            megaLocalStorageGateway.setLastPublicHandleType(MegaApiJava.AFFILIATE_TYPE_CONTACT)
        }

    private suspend fun queryIfIsContact(email: String): Boolean = withContext(ioDispatcher) {
        megaApiGateway.getContact(email)?.visibility == MegaUser.VISIBILITY_VISIBLE
    }
}