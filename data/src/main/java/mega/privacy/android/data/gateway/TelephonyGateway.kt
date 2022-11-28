package mega.privacy.android.data.gateway

/**
 * Telephony Gateway
 */
internal interface TelephonyGateway {

    /**
     * get current country code based on the network
     */
    suspend fun getCurrentCountryCode(): String?

    /**
     * check whether device is in roaming state
     */
    suspend fun isRoaming(): Boolean

    /**
     * format phone number
     * @param number
     * @param countryCode
     */
    suspend fun formatPhoneNumber(number: String, countryCode: String): String

}
