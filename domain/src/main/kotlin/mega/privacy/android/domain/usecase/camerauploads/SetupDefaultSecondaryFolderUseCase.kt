package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import mega.privacy.android.domain.usecase.node.IsNodeInRubbishOrDeletedUseCase
import javax.inject.Inject

/**
 * Setup Default Secondary Folder for Camera Upload
 */
class SetupDefaultSecondaryFolderUseCase @Inject constructor(
    private val cameraUploadsRepository: CameraUploadsRepository,
    private val getUploadFolderHandleUseCase: GetUploadFolderHandleUseCase,
    private val getDefaultNodeHandleUseCase: GetDefaultNodeHandleUseCase,
    private val isNodeInRubbishOrDeletedUseCase: IsNodeInRubbishOrDeletedUseCase,
    private val setupSecondaryFolderUseCase: SetupSecondaryFolderUseCase,
) {

    /**
     *  Invoke
     */
    suspend operator fun invoke() {
        // If there is any possible secondary folder, set it as the default one
        val secondaryFolderHandle =
            getUploadFolderHandleUseCase(cameraUploadFolderType = CameraUploadFolderType.Secondary)
        val defaultNodeHandle =
            getDefaultNodeHandleUseCase(cameraUploadsRepository.getMediaUploadsName())
        if ((secondaryFolderHandle == cameraUploadsRepository.getInvalidHandle()
                    || isNodeInRubbishOrDeletedUseCase(secondaryFolderHandle))
            && defaultNodeHandle != cameraUploadsRepository.getInvalidHandle()
        ) {
            setupSecondaryFolderUseCase(defaultNodeHandle)
        }
    }
}
