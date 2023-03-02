package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.VideoQuality

/**
 * Use Case that retrieves the Video Quality of Videos to be uploaded
 */
fun interface GetUploadVideoQuality {

    /**
     * Invocation function
     *
     * @return The corresponding [VideoQuality], which can be nullable
     */
    suspend operator fun invoke(): VideoQuality?
}