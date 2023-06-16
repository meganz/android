package mega.privacy.android.app.domain.usecase

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Default implementation of [SetupDefaultSecondaryFolder]
 */
class DefaultSetupDefaultSecondaryFolder @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
) : SetupDefaultSecondaryFolder {
    override suspend fun invoke(secondaryFolderName: String) {
        // If there is any possible secondary folder, set it as the default one
        val secondaryFolderHandle =
            getUploadFolderHandleUseCase(cameraUploadFolderType = CameraUploadFolderType.Secondary)
        val defaultNodeHandle = getDefaultNodeHandleUseCase(secondaryFolderName)
        if ((secondaryFolderHandle == cameraUploadRepository.getInvalidHandle()
                    || isNodeInRubbishOrDeletedUseCase(secondaryFolderHandle))
            && defaultNodeHandle != cameraUploadRepository.getInvalidHandle()
        ) {
            setupSecondaryFolderUseCase(defaultNodeHandle)
        }
    }
}
