package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import java.io.File
import javax.inject.Inject

/**
 * Implementation of [GetQRCodeFile]
 */
class DefaultGetQRCodeFile @Inject constructor(
    private val qrCodeRepository: QRCodeRepository,
    private val accountRepository: AccountRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetQRCodeFile {

    companion object {
        private const val QR_IMAGE_FILE_NAME = "QR_code_image.jpg"
    }

    override suspend fun invoke(): File? = withContext(ioDispatcher) {
        accountRepository.accountEmail?.let { email ->
            qrCodeRepository.getQRFile(email + QR_IMAGE_FILE_NAME)
        }
    }
}