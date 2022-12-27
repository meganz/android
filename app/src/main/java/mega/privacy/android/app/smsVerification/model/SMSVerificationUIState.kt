package mega.privacy.android.app.smsVerification.model

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
     * is selected country code valid or not
     */
    val isSelectedCountryCodeValid: Boolean = true,

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
     * is achievement enabled for user
     */
    val isAchievementsEnabled: Boolean = false,

    /**
     * granted bonus storage if phone number is SMS verified
     */
    val bonusStorageSMS: String? = null,
)
