package mega.privacy.android.domain.usecase

/**
 * The interface for stop audio player service
 */
interface StopAudioService {
    /**
     * stop audio player service
     */
    suspend operator fun invoke()
}