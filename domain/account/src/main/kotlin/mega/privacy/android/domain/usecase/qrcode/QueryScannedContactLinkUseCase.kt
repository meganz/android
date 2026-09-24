package mega.privacy.android.domain.usecase.qrcode

import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import javax.inject.Inject

/**
 * Use case for getting contact link for scanned qr code
 */
class QueryScannedContactLinkUseCase @Inject constructor(
    private val repository: QRCodeRepository,
    private val avatarRepository: AvatarRepository,
) {

    /**
     * Invoke
     *
     * @param scannedHandle Base 64 handle of the scanned qr code
     * @return Details of the scanned contact
     */
    suspend operator fun invoke(scannedHandle: String): ScannedContactLinkResult {
        val result = repository.queryScannedContactLink(scannedHandle)

        if (result.qrCodeQueryResult == QRCodeQueryResults.CONTACT_QUERY_OK) {
            repository.updateDatabaseOnQueryScannedContactSuccess(result.handle)
            if (result.isContact) {
                val avatarFile =
                    runCatching { avatarRepository.getAvatarFile(result.email) }
                        .fold(
                            onSuccess = { it },
                            onFailure = { null }
                        )
                val avatarColor = avatarRepository.getAvatarColor(result.handle)

                return result.copy(avatarFile = avatarFile, avatarColor = avatarColor)
            }
        }

        return result
    }
}