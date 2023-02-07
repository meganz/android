package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * [DeleteQRCode] implementation
 */
class DefaultDeleteQRCode @Inject constructor(
    private val accountRepository: AccountRepository,
    private val nodeRepository: NodeRepository,
    private val getQRCodeFile: GetQRCodeFile,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeleteQRCode {

    override suspend fun invoke(contactLink: String) {
        withContext(ioDispatcher) {
            val handle = getHandleByContactLink(contactLink)
            accountRepository.deleteContactLink(handle)
            getQRCodeFile()?.takeIf { it.exists() }?.delete()
        }
    }

    private suspend fun getHandleByContactLink(contactLink: String): Long {
        val contactLinkPrefix = "https://mega.nz/C!"
        val base64Handle = contactLink.substring(contactLinkPrefix.length)
        return nodeRepository.convertBase64ToHandle(base64Handle)
    }
}