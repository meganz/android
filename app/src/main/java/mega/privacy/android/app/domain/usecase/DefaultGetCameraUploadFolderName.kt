package mega.privacy.android.app.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.usecase.GetCameraUploadFolderName
import javax.inject.Inject

/**
 * Get camera upload folder name
 *
 * @return camera upload folder name
 */
class DefaultGetCameraUploadFolderName @Inject constructor(
    @ApplicationContext private val context: Context,
) : GetCameraUploadFolderName {
    override suspend fun invoke(isSecondary: Boolean): String =
        if (!isSecondary) context.getString(R.string.section_photo_sync)
        else context.getString(R.string.section_secondary_media_uploads)
}
