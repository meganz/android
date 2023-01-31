package mega.privacy.android.data.gateway.preferences

/**
 * File Management preferences gateway
 *
 */
internal interface FileManagementPreferencesGateway {
    /**
     * Preference whether to use Mobile data to preview Hi-res images
     *
     * @return [Boolean]
     */
    suspend fun isMobileDataAllowed(): Boolean
}