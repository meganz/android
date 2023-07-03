package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import javax.inject.Inject

/**
 * Check Camera Upload
 */
class AreCameraUploadsFoldersInRubbishBinUseCase @Inject constructor(
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val isNodeInRubbish: IsNodeInRubbish,
) {

    /**
     * Invoke
     * @param primaryHandle Primary Folder Handle
     * @param secondaryHandle Secondary Folder Handle
     * @return true if either the camera uploads primary folder or secondary folder
     *         have been moved to rubbish bin
     */
    suspend operator fun invoke(
        primaryHandle: Long,
        secondaryHandle: Long,
    ): Boolean {
        val isPrimaryFolderInRubbish = isNodeInRubbish(primaryHandle)
        val isSecondaryFolderInRubbish =
            isSecondaryFolderEnabled() && isNodeInRubbish(secondaryHandle)
        return (isPrimaryFolderInRubbish || isSecondaryFolderInRubbish)
    }
}
