package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import javax.inject.Inject

/**
 * Class that implements the use case [CleanOldQRCodeFile]
 */
class DefaultCleanOldQRCodeFile @Inject constructor(
    private val accountRepository: AccountRepository,
    private val qrCodeRepository: QRCodeRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CleanOldQRCodeFile {

    override suspend fun invoke() {
        withContext(ioDispatcher) {
            val oldQRImageFileName = "QRcode.jpg"
            accountRepository.accountEmail?.let { email ->
                qrCodeRepository.getQRFile(
                    fileName = email + oldQRImageFileName
                )?.delete()
            }
        }
    }
}
