package mega.privacy.android.domain.usecase.account.qr

import mega.privacy.android.domain.repository.QRCodeRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case for persisting the rendered QR code image of the logged in account, so that it can be
 * shared or saved later.
 */
class SaveQRCodeImageUseCase @Inject constructor(
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase,
    private val qrCodeRepository: QRCodeRepository,
) {

    /**
     * Invoke.
     *
     * @param imageBytes encoded image content
     * @return the file the image was written to, or null if the account has no QR file location.
     */
    suspend operator fun invoke(imageBytes: ByteArray): File? =
        getQRCodeFileUseCase()?.also { qrCodeRepository.writeQRFile(it, imageBytes) }
}
