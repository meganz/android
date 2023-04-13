package mega.privacy.android.app.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.wrapper.GetFullPathFileWrapper
import mega.privacy.android.domain.repository.CameraUploadRepository
import javax.inject.Inject

/**
 * Get camera upload local path
 *
 * @property cameraUploadRepository
 * @property context
 */
class DefaultGetCameraUploadLocalPath @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getFullPathFileWrapper: GetFullPathFileWrapper,
    @ApplicationContext private val context: Context,
) : GetCameraUploadLocalPath {

    override suspend fun invoke(): String? {
        var localPath = if (cameraUploadRepository.isPrimaryFolderInSDCard()) {
            val sdUri = Uri.parse(cameraUploadRepository.getPrimaryFolderSDCardUriPath())
            getFullPathFileWrapper.getFullPathFromTreeUri(sdUri, context)
        } else {
            cameraUploadRepository.getPrimaryFolderLocalPath()
        }

        if (!localPath?.trim().isNullOrEmpty() && !localPath.isNullOrEmpty()
            && !localPath.endsWith(Constants.SEPARATOR)
        ) {
            localPath += Constants.SEPARATOR
        }
        return localPath
    }
}
