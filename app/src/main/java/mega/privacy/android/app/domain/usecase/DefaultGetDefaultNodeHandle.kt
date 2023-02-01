package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.MegaNodeRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Get the handle by the default path string
 */
class DefaultGetDefaultNodeHandle @Inject constructor(
    private val megaNodeRepository: MegaNodeRepository,
) : GetDefaultNodeHandle {

    override suspend fun invoke(defaultFolderName: String): Long {
        val node = megaNodeRepository.getNodeByPath(defaultFolderName, megaNodeRepository.getRootNode())
        return if (node != null && node.isFolder && !megaNodeRepository.isInRubbish(node)) {
            node.handle
        } else {
            MegaApiJava.INVALID_HANDLE
        }
    }
}
