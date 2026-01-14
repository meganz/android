package mega.privacy.android.feature.payment.usecase

import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import javax.inject.Inject

/**
 * Use case to set the preference for showing external checkout information bottom sheet
 */
class SetExternalCheckoutInformationPreferenceUseCase @Inject constructor(
    private val appPreferencesGateway: AppPreferencesGateway,
) {
    /**
     * Preference key for showing external checkout information
     */
    private val preferenceKey = "external_checkout_show_information"

    /**
     * Set the preference value
     *
     * @param showInformation Whether to show the information bottom sheet next time
     */
    suspend operator fun invoke(showInformation: Boolean) {
        appPreferencesGateway.putBoolean(preferenceKey, showInformation)
    }
}

