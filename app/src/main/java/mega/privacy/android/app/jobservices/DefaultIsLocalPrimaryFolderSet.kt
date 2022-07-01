package mega.privacy.android.app.jobservices

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import mega.privacy.android.app.domain.usecase.GetCameraUploadLocalPath
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Check the availability of camera upload local primary folder
 *
 * If it's a path in internal storage, check its existence
 * If it's a path in SD card, check the corresponding DocumentFile's existence
 *
 * @return true, if local primary folder is available
 */
class DefaultIsLocalPrimaryFolderSet @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val getCameraUploadLocalPath: GetCameraUploadLocalPath,
    @ApplicationContext private val context: Context,
) : IsLocalPrimaryFolderSet {
    override fun invoke(): Boolean {
        return if (cameraUploadRepository.isFolderExternalSd()) {
            val uri = Uri.parse(cameraUploadRepository.getUriExternalSd())
            val file = DocumentFile.fromTreeUri(context, uri)
            if (file == null) {
                Timber.d("Local folder on SD card is unavailable")
                return false
            }
            file.exists()
        } else {
            val localPath = getCameraUploadLocalPath() ?: return false
            File(localPath).exists()
        }
    }
}
