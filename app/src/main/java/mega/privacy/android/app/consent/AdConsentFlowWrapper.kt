package mega.privacy.android.app.consent

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Ad consent flow wrapper - Converts the requestConsentInfoUpdate callback to a flow.
 *
 * @property consentInformation
 */
class AdConsentFlowWrapper @Inject constructor(
    private val consentInformation: ConsentInformation,
) {
    fun getCanRequestConsentFlow(activity: Activity) = callbackFlow {
        val params =
            ConsentRequestParameters.Builder()
                .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { trySend(consentInformation.canRequestAds()) },
            {
                Timber.e("Error loading or showing consent form: ${it.message}")
                trySend(false)
            }
        )

        awaitClose {
            Timber.d("AdConsentFlowWrapper closed")
        }
    }
}