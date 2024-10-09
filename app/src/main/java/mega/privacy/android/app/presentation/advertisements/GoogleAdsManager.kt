package mega.privacy.android.app.presentation.advertisements

import android.app.Activity
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
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

    private var isAdsFeatureEnabled: Boolean = false
    private var isAdRequestAvailable: Boolean = false

    /**
     * Helper variable to get the AdSize for the ads.
     */
    val AD_SIZE: AdSize
        get() = AdSize(320, 50)

    /**
     *  Helper variable to determine if the privacy options form is required.
     */
    val isPrivacyOptionsRequired: Boolean
        get() =
            consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /**
     * Check if an AdRequest is available to be used.
     */
    fun isAdRequestAvailable() = isAdRequestAvailable

    /**
     * Check if the ads feature is enabled.
     */
    suspend fun checkForAdsAvailability() {
        runCatching {
            isAdsFeatureEnabled =
                getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
        }.onFailure {
            isAdsFeatureEnabled = false
            Timber.e(it, "Error getting feature flag value")
        }
    }

    /**
     * Check if ads are enabled.
     */
    fun isAdsEnabled() = isAdsFeatureEnabled

    /**
     * Check the latest consent information from UMP and show the consent form if required.
     * @param activity The activity to check the consent information.
     * @param onConsentInformationUpdated Callback to be called when the consent information is updated.
     */
    suspend fun checkLatestConsentInformation(
        activity: Activity,
        onConsentInformationUpdated: () -> Unit = {},
    ) {
        val params =
            ConsentRequestParameters.Builder()
                .build()
        val shouldShowGenericCookieDialog =
            shouldShowGenericCookieDialogUseCase(getCookieSettingsUseCase())

        val successCallback = ConsentInformation.OnConsentInfoUpdateSuccessListener {
            if (shouldShowGenericCookieDialog.not()) {
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
            }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            successCallback,
            failureCallback
        )
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
    fun fetchAdRequest(): AdManagerAdRequest? {
        return if (isAdsEnabled() && consentInformation.canRequestAds()) {
            isAdRequestAvailable = true
            AdManagerAdRequest.Builder().build()
        } else {
            isAdRequestAvailable = false
            null
        }
    }

}