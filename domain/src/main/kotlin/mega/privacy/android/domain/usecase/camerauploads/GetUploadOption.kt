package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption

/**
 * Use Case to retrieve the upload option for Camera Uploads
 */
fun interface GetUploadOption {

    /**
     * Invocation function
     *
     * @return [UploadOption]
     */
    suspend operator fun invoke(): UploadOption
}