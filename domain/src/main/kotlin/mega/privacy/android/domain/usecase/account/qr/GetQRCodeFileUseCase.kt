package mega.privacy.android.domain.usecase.account.qr

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import java.io.File
import javax.inject.Inject

/**
 * Use case for getting the QR code file of the logged in account.
 */
class GetQRCodeFileUseCase @Inject constructor(
    private val qrCodeRepository: QRCodeRepository,
    private val accountRepository: AccountRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Invoke.
     *
     * @return QR file. A non-null file is returned even if it does not exist.
     */
    suspend operator fun invoke(): File? = withContext(ioDispatcher) {
        accountRepository.getAccountEmail()?.let { email ->
            qrCodeRepository.getQRFile(email + QR_IMAGE_FILE_NAME)
        }
    }

    companion object {
        private const val QR_IMAGE_FILE_NAME = "QR_code_image.jpg"
    }
}