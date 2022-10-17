package mega.privacy.android.app.domain.usecase

import mega.privacy.android.data.repository.FilesRepository
import nz.mega.sdk.MegaApiJava
import javax.inject.Inject

/**
 * Get the handle by the default path string
 */
class DefaultGetDefaultNodeHandle @Inject constructor(
    private val filesRepository: FilesRepository,
) : GetDefaultNodeHandle {

    override suspend fun invoke(defaultFolderName: String): Long {
        val node = filesRepository.getNodeByPath(defaultFolderName, filesRepository.getRootNode())
        return if (node != null && node.isFolder && !filesRepository.isInRubbish(node)) {
            node.handle
        } else {
            MegaApiJava.INVALID_HANDLE
        }
    }
}
