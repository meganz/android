package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.repository.QRCodeRepository
import javax.inject.Inject

/**
 * Use case implementation for getting contact link for scanned qr code
 */
class DefaultQueryScannedContactLink @Inject constructor(
    private val repository: QRCodeRepository,
) : QueryScannedContactLink {

    override suspend fun invoke(scannedHandle: String): ScannedContactLinkResult {
        val result = repository.queryScannedContactLink(scannedHandle)

        if (result.qrCodeQueryResult == QRCodeQueryResults.CONTACT_QUERY_OK)
            repository.updateDatabaseOnQueryScannedContactSuccess(result.handle)

        return result
    }
}