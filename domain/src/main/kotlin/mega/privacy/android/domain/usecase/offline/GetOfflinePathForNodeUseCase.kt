package mega.privacy.android.domain.usecase.offline

import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.usecase.favourites.GetOfflineFileUseCase
import java.io.File
import javax.inject.Inject

/**
 * Get the path where node should be downloaded for offline
 */
class GetOfflinePathForNodeUseCase @Inject constructor(
    private val getOfflineFileUseCase: GetOfflineFileUseCase,
    private val getOfflineNodeInformationUseCase: GetOfflineNodeInformationUseCase,
) {
    /**
     * Get the path where node should be downloaded for offline
     * @param node [Node] which path will be returned
     */
    suspend operator fun invoke(node: Node) =
        getOfflineFileUseCase(getOfflineNodeInformationUseCase(node))
            .parentFile?.path?.ensureEndsWithFileSeparator()

    private fun String.ensureEndsWithFileSeparator() =
        if (this.endsWith(File.separator)) {
            this
        } else {
            this.plus(File.separator)
        }
}