package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Use Case to set the Upload Option of Camera Uploads
 */
fun interface SetUploadOption {

    /**
     * Invocation function
     *
     * @param uploadOption The [UploadOption] to set
     */
    suspend operator fun invoke(uploadOption: UploadOption)
}