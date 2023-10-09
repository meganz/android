package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.repository.NodeRepository
import javax.inject.Inject

/**
 * Get Handle From Contact Link Use Case
 * For example contact link https://mega.nz/C!86YkxIDC
 */
class GetHandleFromContactLinkUseCase @Inject constructor(
    private val nodeRepository: NodeRepository,
) {
    /**
     * Invoke
     *
     */
    suspend operator fun invoke(link: String): Long = getBase64Handle(link)?.let {
        nodeRepository.convertBase64ToHandle(it)
    } ?: nodeRepository.getInvalidHandle()


    private fun getBase64Handle(link: String) =
        link.split("C!".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray().getOrNull(1)
}