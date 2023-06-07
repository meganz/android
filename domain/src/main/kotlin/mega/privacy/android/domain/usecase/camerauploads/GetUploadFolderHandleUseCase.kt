package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import javax.inject.Inject


/**
 * Use Case for getting Either Primary or Secondary Folder Handle
 */
class GetUploadFolderHandleUseCase @Inject constructor(
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase,
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase,
) {

    /**
     * @param cameraUploadFolderType [CameraUploadFolderType]
     */
    suspend operator fun invoke(cameraUploadFolderType: CameraUploadFolderType) =
        when (cameraUploadFolderType) {
            CameraUploadFolderType.Primary -> getPrimarySyncHandleUseCase()
            CameraUploadFolderType.Secondary -> getSecondarySyncHandleUseCase()
        }
}
