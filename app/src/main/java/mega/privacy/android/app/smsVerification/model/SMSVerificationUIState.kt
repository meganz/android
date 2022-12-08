package mega.privacy.android.app.smsVerification.model

/**
 * SMSVerificationUIState
 */
data class SMSVerificationUIState(
    /**
     * phone number
     */
    val phoneNumber: String,

    /**
     * is phone number valid or not
     */
    val isPhoneNumberValid: Boolean,

    /**
     * country code for network info
     */
    val inferredCountryCode: String,

    /**
     * selected country code
     */
    val selectedCountryCode: String,

    /**
     * is selected country code valid or not
     */
    val isSelectedCountryCodeValid: Boolean,

    /**
     * selected country name
     */
    val selectedCountryName: String,

    /**
     * selected dial code
     */
    val selectedDialCode: String,

    /**
     * is user locked or not
     */
    val isUserLocked: Boolean,
    /**
     * country calling codes
     */
    val countryCallingCodes: List<String>,

    /**
     * is achievement enabled for user
     */
    val isAchievementsEnabled: Boolean,
)
