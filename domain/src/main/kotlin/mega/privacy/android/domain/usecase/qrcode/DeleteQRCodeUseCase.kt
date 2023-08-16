package mega.privacy.android.domain.usecase.qrcode

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import javax.inject.Inject

/**
 * Use case to delete QR Code and file
 */
class DeleteQRCodeUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val nodeRepository: NodeRepository,
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Invoke method
     *
     * @param contactLink handle – Contact link to be deleted. It is the URL starts with "https://"
     *
     */
    suspend operator fun invoke(contactLink: String) {
        withContext(ioDispatcher) {
            val handle = getHandleByContactLink(contactLink)
            accountRepository.deleteContactLink(handle)
            getQRCodeFileUseCase()?.takeIf { it.exists() }?.delete()
        }
    }

    private suspend fun getHandleByContactLink(contactLink: String): Long {
        val contactLinkPrefix = "https://mega.nz/C!"
        val base64Handle = contactLink.substring(contactLinkPrefix.length)
        return nodeRepository.convertBase64ToHandle(base64Handle)
    }
}