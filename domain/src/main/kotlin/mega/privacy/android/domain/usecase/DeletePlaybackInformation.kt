package mega.privacy.android.domain.usecase

/**
 * The use case for deleting playback information
 */
fun interface DeletePlaybackInformation {

    /**
     * Delete playback information
     *
     * @param mediaId the media id of deleted item
     */
    suspend operator fun invoke(mediaId: Long)
}