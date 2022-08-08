package mega.privacy.android.app.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.wrapper.GetFullPathFileWrapper
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get camera upload secondary local path
 *
 * @property cameraUploadRepository
 * @property context
 */
class DefaultGetCameraUploadLocalPathSecondary @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getFullPathFileWrapper: GetFullPathFileWrapper,
    @ApplicationContext private val context: Context,
) : GetCameraUploadLocalPathSecondary {

    override fun invoke(): String? {
        var localPath = if (cameraUploadRepository.isMediaFolderExternalSd()) {
            val uri = Uri.parse(cameraUploadRepository.getUriMediaFolderExternalSd())
            getFullPathFileWrapper.getFullPathFromTreeUri(uri, context)
        } else {
            cameraUploadRepository.getSecondaryFolderPath()
        }

        if (localPath != null && !localPath.endsWith(Constants.SEPARATOR)) {
            localPath += Constants.SEPARATOR
        }
        return localPath
    }
}
