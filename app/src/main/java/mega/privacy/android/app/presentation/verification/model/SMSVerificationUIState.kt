package mega.privacy.android.app.presentation.verification.model

/**
 * SMSVerificationUIState
 */
data class SMSVerificationUIState(
    /**
     * phone number
     */
    val phoneNumber: String = "",

    /**
     * is phone number valid or not
     */
    val isPhoneNumberValid: Boolean = true,

    /**
     * country code for network info
     */
    val inferredCountryCode: String = "",

    /**
     * selected country code
     */
    val selectedCountryCode: String = "",

    /**
     * selected country name
     */
    val selectedCountryName: String = "",

    /**
     * selected dial code
     */
    val selectedDialCode: String = "",

    /**
     * is user locked or not
     */
    val isUserLocked: Boolean = false,
    /**
     * country calling codes
     */
    val countryCallingCodes: List<String> = emptyList(),

    /**
     * info text
     */
    val infoText: String = "",

    /**
     * header text
     */
    val headerText: String = "",

    /**
     * country code text
     */
    val countryCodeText: String = "",

    /**
     * country code text
     */
    val phoneNumberErrorText: String = "",

    /**
     * whether verification code sent or not
     */
    val isVerificationCodeSent: Boolean = false,

    /**
     * whether sent button should be disabled or not
     */
    val isNextEnabled: Boolean = true,

    ) {
    /**
     *  is selected country code valid or not
     */
    val isCountryCodeValid =
        selectedCountryCode.isNotEmpty() && selectedDialCode.isNotEmpty() && selectedCountryName.isNotEmpty()
}
