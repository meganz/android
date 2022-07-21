package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecordType
import mega.privacy.android.domain.usecase.MediaLocalPathExists
import javax.inject.Inject

/**
 * Does media local path exists
 *
 */
class DefaultMediaLocalPathExists @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : MediaLocalPathExists {

    override fun invoke(filePath: String, isSecondary: Boolean): Boolean =
        cameraUploadRepository.doesLocalPathExist(filePath,
            isSecondary,
            SyncRecordType.TYPE_ANY.value)
}
