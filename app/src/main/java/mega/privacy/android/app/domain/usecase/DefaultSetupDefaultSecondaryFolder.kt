package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.GetUploadFolderHandle
import mega.privacy.android.domain.usecase.IsNodeInRubbishOrDeleted
import mega.privacy.android.domain.usecase.SetupSecondaryFolder
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import javax.inject.Inject

/**
 * Default implementation of [SetupDefaultSecondaryFolder]
 */
class DefaultSetupDefaultSecondaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadFolderHandle: GetUploadFolderHandle,
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase,
    private val isNodeInRubbishOrDeleted: IsNodeInRubbishOrDeleted,
    private val setupSecondaryFolder: SetupSecondaryFolder,
) : SetupDefaultSecondaryFolder {
    override suspend fun invoke(secondaryFolderName: String) {
        // If there is any possible secondary folder, set it as the default one
        val secondaryFolderHandle = getUploadFolderHandle(isPrimary = false)
        val defaultNodeHandle = getDefaultNodeHandleUseCase(secondaryFolderName)
        if ((secondaryFolderHandle == cameraUploadRepository.getInvalidHandle()
                    || isNodeInRubbishOrDeleted(secondaryFolderHandle))
            && defaultNodeHandle != cameraUploadRepository.getInvalidHandle()
        ) {
            setupSecondaryFolder(defaultNodeHandle)
        }
    }
}
