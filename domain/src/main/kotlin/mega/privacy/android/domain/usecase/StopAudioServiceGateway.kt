package mega.privacy.android.domain.usecase

/**
 * The interface for stop audio player service
 */
interface StopAudioServiceGateway {
    /**
     * stop audio player service
     */
    suspend operator fun invoke()
}