package mega.privacy.android.data.wrapper

import mega.privacy.android.domain.exception.MegaException

/**
 * The interface for wrapping the static method regarding String
 */
interface StringWrapper {


    /**
     *Get localized progress size
     */
    fun getProgressSize(progress: Long, size: Long): String

    /**
     * Encode Base64 string
     *
     * @param string    String to be encoded
     * @return          Encoded Base64 string
     */
    fun encodeBase64(string: String): String

    /**
     * Decode Base64 string
     *
     * @param base64    Base64 string to be decoded
     * @return          Decoded base64 string
     */
    fun decodeBase64(base64: String): String

    /**
     * Wrapping getSizeString function
     * @return size string
     */
    fun getSizeString(size: Long): String

    /**
     * Get error string resource id
     * @param megaException [MegaException]
     */
    fun getErrorStringResource(megaException: MegaException): String

    /**
     * Get section_cloud_drive string resource
     */
    fun getCloudDriveSection(): String

    /**
     * Get section_rubbish_bin string resource
     */
    fun getRubbishBinSection(): String

    /**
     * Get title_incoming_shares_explorer string resource
     */
    fun getTitleIncomingSharesExplorer(): String

    /**
     * Get error_share_owner_storage_quota string resource
     */
    fun getErrorStorageQuota(): String

    /**
     * Get section_saved_for_offline_new string resource
     */
    fun getSavedForOfflineNew(): String
}
