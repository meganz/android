package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.usecase.FileNameExists
import javax.inject.Inject

/**
 * Does sync record file name exist
 *
 */
class DefaultFileNameExists @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : FileNameExists {

    override fun invoke(
        fileName: String,
        isSecondary: Boolean,
    ): Boolean = cameraUploadRepository.doesFileNameExist(fileName,
        isSecondary,
        SyncRecordType.TYPE_ANY.value)
}
