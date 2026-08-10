package mega.privacy.android.data.facade

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import mega.privacy.android.data.gateway.FirebaseAnalyticsGateway
import javax.inject.Inject

internal class FirebaseAnalyticsFacade @Inject constructor() : FirebaseAnalyticsGateway {

    override fun logEvent(eventName: String) {
        Firebase.analytics.logEvent(eventName, null)
    }
}
