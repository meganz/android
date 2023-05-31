package mega.privacy.android.domain.usecase.account.qr

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.QRCodeRepository
import javax.inject.Inject

/**
 * Use case for checking if the current uploaded file is a QR code file but not of the logged in account.
 * If the file is not of the logged in account, it is removed from local.
 */
class CheckUploadedQRCodeFileUseCase @Inject constructor(
    private val qrCodeRepository: QRCodeRepository,
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke.
     *
     * @param uploadedFileName Name of the uploaded file.
     * @return True if and only if the file or directory is successfully deleted, false otherwise.
     */
    suspend operator fun invoke(uploadedFileName: String) = withContext(ioDispatcher) {
        val qRCodeFileName = getQRCodeFileUseCase()?.name
        qrCodeRepository.getQRFile(uploadedFileName)
            ?.takeIf { it.exists() && it.name != qRCodeFileName }
            ?.delete()
            ?: false
    }
}