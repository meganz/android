package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject

/**
 * Map [MegaRequest] and [MegaError] to [ScannedContactLinkResult]
 */
internal class ScannedContactLinkResultMapper @Inject constructor() {

    operator fun invoke(
        request: MegaRequest,
        error: MegaError,
        isContact: Boolean
    ): ScannedContactLinkResult {

        val qrCodeQueryResult = when (error.errorCode) {
            MegaError.API_OK -> QRCodeQueryResults.CONTACT_QUERY_OK
            MegaError.API_EEXIST -> QRCodeQueryResults.CONTACT_QUERY_EEXIST
            else -> QRCodeQueryResults.CONTACT_QUERY_DEFAULT
        }

        return ScannedContactLinkResult(
            contactName = "${request.name} ${request.text}",
            email = request.email,
            handle = request.nodeHandle,
            isContact = isContact,
            qrCodeQueryResult = qrCodeQueryResult
        )
    }
}
