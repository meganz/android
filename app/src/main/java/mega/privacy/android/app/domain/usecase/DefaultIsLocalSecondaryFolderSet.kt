package mega.privacy.android.app.domain.usecase

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.utils.wrapper.GetDocumentFileWrapper
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Check the availability of camera upload local secondary folder
 *
 * If it's a path in internal storage, check its existence
 * If it's a path in SD card, check the corresponding DocumentFile's existence
 *
 * @return true, if secondary folder is available
 */
class DefaultIsLocalSecondaryFolderSet @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val getDocumentFileWrapper: GetDocumentFileWrapper,
    @ApplicationContext private val context: Context,
) : IsLocalSecondaryFolderSet {
    override suspend fun invoke(): Boolean {
        return if (isSecondaryFolderEnabled()) {
            if (cameraUploadRepository.isSecondaryFolderInSDCard()) {
                val uri = Uri.parse(cameraUploadRepository.getSecondaryFolderSDCardUriPath())
                val file = getDocumentFileWrapper.getDocumentFileFromTreeUri(context, uri)
                if (file == null) {
                    Timber.d("Local Media Folder on SD card is unavailable")
                    return false
                }
                file.exists()
            } else {
                val localPathSecondary = getSecondaryFolderPathUseCase()
                File(localPathSecondary).exists()
            }
        } else {
            Timber.d("Not enabled Secondary Folder Upload")
            cameraUploadRepository.setSecondaryEnabled(false)
            true
        }
    }
}
