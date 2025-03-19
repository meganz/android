package mega.privacy.android.app.presentation.advertisements

import android.app.Activity
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.ShouldShowGenericCookieDialogUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Class to manage Google Ads and User consent information.
 *
 * @property consentInformation The ConsentInformation instance.
 * @property getFeatureFlagValueUseCase The use case to get the feature flag value.
 * @property getCookieSettingsUseCase The use case to get the cookie settings.
 * @property shouldShowGenericCookieDialogUseCase The use case to check if the generic cookie dialog should be shown.
 */
class GoogleAdsManager @Inject constructor(
    private val consentInformation: ConsentInformation,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val shouldShowGenericCookieDialogUseCase: ShouldShowGenericCookieDialogUseCase,
) {

    private val _isAdsFeatureEnabled = MutableStateFlow(false)

    /**
     * Flow to provide if the ads feature is enabled.
     */
    val isAdsFeatureEnabled = _isAdsFeatureEnabled.asStateFlow()

    private val _request = MutableStateFlow<AdManagerAdRequest?>(null)

    /**
     * Flow to provide the AdRequest to be used in the AdManager.
     */
    val request = _request.asStateFlow()

    /**
     *  Helper variable to determine if the privacy options form is required.
     */
    val isPrivacyOptionsRequired: Boolean
        get() =
            consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Check if the ads feature is enabled.
     */
    suspend fun checkForAdsAvailability() {
        runCatching {
            _isAdsFeatureEnabled.value =
                getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
            Timber.d("Ads feature enabled: $isAdsFeatureEnabled")
        }.onFailure {
            _isAdsFeatureEnabled.value = false
            Timber.e(it, "Error getting feature flag value")
        }
    }

    /**
     * Check if ads are enabled.
     */
    fun isAdsEnabled() = _isAdsFeatureEnabled.value

    /**
     * Check if ads are ready to be requested.
     */
    fun hasAdsRequest() = _request.value != null

    /**
     * Check the latest consent information from UMP and show the consent form if required.
     * @param activity The activity to check the consent information.
     * @param forceAskConsent True to force showing the consent form, false otherwise. it avoids showing the dialog if the user is viewing the generic cookie dialog.
     * @param onConsentInformationUpdated Callback to be called when the consent information is updated.
     */
    suspend fun checkLatestConsentInformation(
        activity: Activity,
        forceAskConsent: Boolean = true,
        onConsentInformationUpdated: () -> Unit = { fetchAdRequest() },
    ) {
        Timber.d("Checking latest consent information")
        val params =
            ConsentRequestParameters.Builder()
                .build()

        if (consentInformation.canRequestAds()) {
            Timber.d("Consent information is ready")
            onConsentInformationUpdated()
        } else {
            val shouldShowGenericCookieDialog = runCatching {
                // non-logged in user can't see the dialog
                shouldShowGenericCookieDialogUseCase(getCookieSettingsUseCase())
            }.getOrElse { false }

            val successCallback = ConsentInformation.OnConsentInfoUpdateSuccessListener {
                Timber.d("success loading consent $shouldShowGenericCookieDialog")
                if (forceAskConsent || shouldShowGenericCookieDialog.not()) {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError: FormError? ->
                        if (loadAndShowError != null) {
                            Timber.e("Error loading or showing consent form: ${loadAndShowError.message}")
                        }
                        onConsentInformationUpdated()
                    }
                }
            }
            val failureCallback =
                ConsentInformation.OnConsentInfoUpdateFailureListener { fromError: FormError ->
                    Timber.e("Error loading or showing consent form: ${fromError.message}")
                    onConsentInformationUpdated()
                }
            Timber.d("Requesting consent information")
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                successCallback,
                failureCallback
            )
        }
    }

    /**
     * Show the privacy options form.
     * @param activity The activity to show the form.
     * @param onConsentFormDismissed Callback to be called when the form is dismissed.
     */
    fun showPrivacyOptionsForm(activity: Activity, onConsentFormDismissed: () -> Unit = {}) {

        UserMessagingPlatform.showPrivacyOptionsForm(activity) { fromError: FormError? ->
            if (fromError != null) {
                Timber.e("Error loading or showing consent form: ${fromError.message}")
            }
            onConsentFormDismissed()
        }
    }

    /**
     * Fetch an AdRequest to be used in the AdManager.
     */
    fun fetchAdRequest() {
        Timber.d("Fetching AdRequest")
        if (isAdsEnabled() && consentInformation.canRequestAds()) {
            _request.update { AdManagerAdRequest.Builder().build() }
        } else {
            _request.update { null }
        }
    }
}