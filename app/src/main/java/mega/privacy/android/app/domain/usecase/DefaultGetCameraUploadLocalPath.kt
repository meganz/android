package mega.privacy.android.app.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import javax.inject.Inject

/**
 * Get camera upload local path
 *
 * @property cameraUploadRepository
 * @property context
 */
class DefaultGetCameraUploadLocalPath @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    @ApplicationContext private val context: Context,
) : GetCameraUploadLocalPath {

    override fun invoke(): String? {
        var localPath = if (cameraUploadRepository.isFolderExternalSd()) {
            val sdUri = Uri.parse(cameraUploadRepository.getUriExternalSd())
            FileUtil.getFullPathFromTreeUri(sdUri, context)
        } else {
            cameraUploadRepository.getSyncLocalPath()
        }

        if (!localPath?.trim().isNullOrEmpty() && !localPath.isNullOrEmpty()
            && !localPath.endsWith(Constants.SEPARATOR)
        ) {
            localPath += Constants.SEPARATOR
        }
        return localPath
    }
}
