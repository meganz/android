package mega.privacy.android.feature.payment.usecase

import kotlinx.coroutines.flow.first
import mega.privacy.android.data.gateway.preferences.AppPreferencesGateway
import javax.inject.Inject

/**
 * Use case to get the preference for showing external checkout information bottom sheet
 */
class GetExternalCheckoutInformationPreferenceUseCase @Inject constructor(
    private val appPreferencesGateway: AppPreferencesGateway,
) {
    /**
     * Preference key for showing external checkout information
     */
    private val preferenceKey = "external_checkout_show_information"

    /**
     * Get the preference value
     *
     * @return Boolean indicating whether to show the information (default: true)
     */
    suspend operator fun invoke(): Boolean =
        appPreferencesGateway.monitorBoolean(preferenceKey, defaultValue = true).first()
}

