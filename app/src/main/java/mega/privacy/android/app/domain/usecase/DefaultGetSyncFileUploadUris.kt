package mega.privacy.android.app.domain.usecase

import android.net.Uri
import android.provider.MediaStore
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.domain.repository.CameraUploadRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Get sync file upload uris
 *
 */
class DefaultGetSyncFileUploadUris @Inject constructor(
    private val cameraUploadRepository: CameraUploadRepository,
) : GetSyncFileUploadUris {
    override fun invoke(): List<Uri> {

        val uris = mutableListOf<Uri>()
        val fileUpload = cameraUploadRepository.getSyncFileUpload()
        if (fileUpload != null && fileUpload.toIntOrNull() != null) {
            when (fileUpload.toInt()) {
                MegaPreferences.ONLY_PHOTOS -> {
                    Timber.d("Only Upload Photo")
                    uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                }
                MegaPreferences.ONLY_VIDEOS -> {
                    Timber.d("Only Upload Video")
                    uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI)
                }
                MegaPreferences.PHOTOS_AND_VIDEOS -> {
                    Timber.d("Upload Photo and Video")
                    uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI)
                }
            }
        } else {
            cameraUploadRepository.setPhotosSyncFileUpload()
            Timber.d("Sync File Upload Setting is NULL, Only Upload Photo")
            uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        }
        return uris
    }
}
