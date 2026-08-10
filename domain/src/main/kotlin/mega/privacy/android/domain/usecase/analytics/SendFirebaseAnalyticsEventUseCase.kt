package mega.privacy.android.domain.usecase.analytics

import mega.privacy.android.domain.entity.analytics.FirebaseAnalyticsEvent
import mega.privacy.android.domain.repository.FirebaseAnalyticsRepository
import javax.inject.Inject

/**
 * Use case to send an event to Firebase Analytics
 *
 * Intended for events consumed by Firebase A/B Testing experiments.
 */
class SendFirebaseAnalyticsEventUseCase @Inject constructor(
    private val firebaseAnalyticsRepository: FirebaseAnalyticsRepository,
) {
    /**
     * Invoke
     *
     * @param event Event to log
     */
    operator fun invoke(event: FirebaseAnalyticsEvent) =
        firebaseAnalyticsRepository.logEvent(event)
}
