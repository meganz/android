package mega.privacy.android.domain.usecase.notifications

/**
 * Get feature notification count use case
 */
interface GetFeatureNotificationCountUseCase {

    /**
     * Invoke
     *
     * @return Int notification count
     */
    suspend operator fun invoke(): Int
}