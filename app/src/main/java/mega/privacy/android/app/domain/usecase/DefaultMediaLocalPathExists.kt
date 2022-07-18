package mega.privacy.android.app.domain.usecase

import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.entity.SyncRecordType
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
