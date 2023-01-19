package mega.privacy.android.domain.usecase

/**
 * The use case for saving playback times
 */
fun interface SavePlaybackTimes {

    /**
     * Save the playback times
     */
    suspend operator fun invoke()
}