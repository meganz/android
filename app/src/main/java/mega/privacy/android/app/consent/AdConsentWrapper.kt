package mega.privacy.android.app.consent

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.advertisements.SetGoogleConsentLoadedUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import timber.log.Timber
import javax.inject.Inject

/**
 * Ad consent flow wrapper - Converts the requestConsentInfoUpdate callback to a flow.
 *
 * @property consentInformation
 */
class AdConsentWrapper @Inject constructor(
    private val consentInformation: ConsentInformation,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val appDialogEventQueue: AppDialogsEventQueue,
    private val setGoogleConsentLoadedUseCase: SetGoogleConsentLoadedUseCase,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) {
    fun getCanRequestConsentFlow(activity: Activity) = callbackFlow {
        val params =
            ConsentRequestParameters.Builder()
                .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            { trySend(true) },
            {
                Timber.e("Error loading or showing consent form: ${it.message}")
                trySend(false)
            }
        )

        awaitClose {
            Timber.d("AdConsentFlowWrapper closed")
        }
    }

    fun refreshConsent() {
        coroutineScope.launch {
            runCatching {
                if (getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)) {
                    if (consentInformation.canRequestAds()) {
                        setGoogleConsentLoadedUseCase(true)
                    } else {
                        appDialogEventQueue.emit(AppDialogEvent(AdConsentDialog))
                    }
                }
            }.onFailure { Timber.e(it, "Error in refreshing ad consent") }
        }
    }
}