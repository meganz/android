package mega.privacy.android.domain.usecase.contact

import mega.privacy.android.domain.usecase.GetCurrentCountryCodeUseCase
import mega.privacy.android.domain.usecase.verification.GetFormattedPhoneNumberUseCase
import javax.inject.Inject

/**
 * Use case to get normalized phone number by network
 */
class GetNormalizedPhoneNumberByNetworkUseCase @Inject constructor(
    private val getFormattedPhoneNumberUseCase: GetFormattedPhoneNumberUseCase,
    private val getCurrentCountryCodeUseCase: GetCurrentCountryCodeUseCase,
) {

    /**
     * Invocation method to normalized the phone number by network
     */
    suspend operator fun invoke(phoneNumber: String): String? {
        val countryCode = getCurrentCountryCodeUseCase()
        return countryCode?.let {
            getFormattedPhoneNumberUseCase(phoneNumber, countryCode)
        }
    }
}
