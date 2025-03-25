package mega.privacy.android.domain.usecase.qrcode

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.account.qr.GetQRCodeFileUseCase
import mega.privacy.android.domain.usecase.contact.DeleteContactLinkUseCase
import javax.inject.Inject

/**
 * Use Case to delete the QR Code and its file
 */
class DeleteQRCodeUseCase @Inject constructor(
    private val deleteContactLinkUseCase: DeleteContactLinkUseCase,
    private val nodeRepository: NodeRepository,
    private val getQRCodeFileUseCase: GetQRCodeFileUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * Invocation function
     *
     * @param contactLink handle – Contact link to be deleted. It is the URL starting with "https://"
     */
    suspend operator fun invoke(contactLink: String) {
        withContext(ioDispatcher) {
            val handle = getHandleByContactLink(contactLink)
            deleteContactLinkUseCase(handle)
            getQRCodeFileUseCase()?.takeIf { it.exists() }?.delete()
        }
    }

    private suspend fun getHandleByContactLink(contactLink: String): Long {
        val contactLinkPrefix = "https://mega.nz/C!"
        val base64Handle = contactLink.substring(contactLinkPrefix.length)
        return nodeRepository.convertBase64ToHandle(base64Handle)
    }
}