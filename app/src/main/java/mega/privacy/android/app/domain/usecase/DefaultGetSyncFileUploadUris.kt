package mega.privacy.android.app.domain.usecase

import android.net.Uri
import android.provider.MediaStore
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.usecase.camerauploads.GetUploadOptionUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Default implementation of [GetSyncFileUploadUris]
 *
 * @property getUploadOptionUseCase [GetUploadOptionUseCase]
 */
class DefaultGetSyncFileUploadUris @Inject constructor(
    private val getUploadOptionUseCase: GetUploadOptionUseCase,
) : GetSyncFileUploadUris {
    override suspend fun invoke(): List<Uri> {
        val uris = mutableListOf<Uri>()
        when (getUploadOptionUseCase()) {
            UploadOption.PHOTOS -> {
                Timber.d("Only Upload Photos")
                uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            }
            UploadOption.VIDEOS -> {
                Timber.d("Only Upload Videos")
                uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI)
            }
            UploadOption.PHOTOS_AND_VIDEOS -> {
                Timber.d("Upload both Photos and Videos")
                uris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                uris.add(MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                uris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                uris.add(MediaStore.Video.Media.INTERNAL_CONTENT_URI)
            }
        }
        return uris
    }
}
