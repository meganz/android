package mega.privacy.android.data.repository

import mega.privacy.android.data.gateway.FirebaseAnalyticsGateway
import mega.privacy.android.domain.entity.analytics.FirebaseAnalyticsEvent
import mega.privacy.android.domain.repository.FirebaseAnalyticsRepository
import javax.inject.Inject

internal class FirebaseAnalyticsRepositoryImpl @Inject constructor(
    private val firebaseAnalyticsGateway: FirebaseAnalyticsGateway,
) : FirebaseAnalyticsRepository {

    override fun logEvent(event: FirebaseAnalyticsEvent) {
        firebaseAnalyticsGateway.logEvent(event.eventName)
    }
}
