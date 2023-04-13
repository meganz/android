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

    override suspend fun invoke(): String? {
        var localPath = if (cameraUploadRepository.isSecondaryFolderInSDCard()) {
            val uri = Uri.parse(cameraUploadRepository.getSecondaryFolderSDCardUriPath())
            getFullPathFileWrapper.getFullPathFromTreeUri(uri, context)
        } else {
            cameraUploadRepository.getSecondaryFolderLocalPath()
        }

        if (localPath != null && !localPath.endsWith(Constants.SEPARATOR)) {
            localPath += Constants.SEPARATOR
        }
        return localPath
    }
}
