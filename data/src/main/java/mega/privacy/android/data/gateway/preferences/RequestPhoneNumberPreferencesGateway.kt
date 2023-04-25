package mega.privacy.android.data.gateway.preferences

/**
 * Request Phone Number Preference Gateway
 */
interface RequestPhoneNumberPreferencesGateway {

    /**
     * Sets requesting the phone number to the passed flag parameter in the data store,
     * @param isShown boolean flag to request phone number or not
     */
    suspend fun setRequestPhoneNumberPreference(isShown: Boolean)

    /**
     * Checks if can request phone number or not from Data Store,The default value is set to false
     * @return [Boolean] flag to request phone number
     */
    suspend fun isRequestPhoneNumberPreferenceShown(): Boolean
}
