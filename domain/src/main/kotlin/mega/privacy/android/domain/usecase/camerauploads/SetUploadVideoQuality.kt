package mega.privacy.android.domain.usecase.camerauploads

import mega.privacy.android.domain.entity.VideoQuality

/**
 * Use Case to set the Video Quality of Videos to be uploaded
 */
fun interface SetUploadVideoQuality {

    /**
     * Invocation function
     *
     * @param videoQuality The new [VideoQuality]
     */
    suspend operator fun invoke(videoQuality: VideoQuality)
}