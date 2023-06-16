package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.camerauploads.GetDefaultNodeHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetUploadFolderHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.SetupSecondaryFolderUseCase
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Setup Default Secondary Folder for Camera Upload
 */
class SetupDefaultSecondaryFolderUseCase @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
) {

    /**
     *  Invoke
     *
     *  @param secondaryFolderName
     */
    suspend operator fun invoke(secondaryFolderName: String) {
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
