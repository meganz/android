package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.QRCodeRepository
import javax.inject.Inject

/**
 * [DeleteQRCode] implementation
 */
class DefaultDeleteQRCode @Inject constructor(
    private val accountRepository: AccountRepository,
    private val nodeRepository: NodeRepository,
    private val qrCodeRepository: QRCodeRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeleteQRCode {

    override suspend fun invoke(contactLink: String, fileName: String) {
        withContext(ioDispatcher) {
            val contactLinkPrefix = "https://mega.nz/C!"
            val base64Handle = contactLink.substring(contactLinkPrefix.length)
            val handle = nodeRepository.convertBase64ToHandle(base64Handle)
            accountRepository.deleteContactLink(handle)
            qrCodeRepository.getQRFile(fileName)?.takeIf { it.exists() }?.delete()
        }
    }
}