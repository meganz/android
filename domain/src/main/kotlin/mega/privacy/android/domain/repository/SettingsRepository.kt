package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsSoundNotifications
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.preference.StartScreen
import java.io.File

/**
 * Settings repository - class for handling all calls relating to settings
 *
 */
interface SettingsRepository {

    /**
     * Is passcode lock preference enabled
     *
     * @return true if enabled
     */
    fun isPasscodeLockPreferenceEnabled(): Boolean

    /**
     * Set passcode lock enabled/disabled
     *
     * @param enabled
     */
    fun setPasscodeLockEnabled(enabled: Boolean)

    /**
     * Set the passcode lock code
     */
    suspend fun setPasscodeLockCode(passcodeLockCode: String)

    /**
     * Fetch contact links option
     *
     * @return true if option is enabled, else false
     */
    suspend fun fetchContactLinksOption(): Boolean

    /**
     * Set auto accept qr requests
     *
     * @param accept
     * @return true if option is enabled, else false
     */
    suspend fun setAutoAcceptQR(accept: Boolean): Boolean

    /**
     * Monitor hide recent activity setting
     *
     * @return hide recent activity option enabled status as a flow
     */
    fun monitorHideRecentActivity(): Flow<Boolean?>

    /**
     * Set hide recent activity
     *
     * @param value
     */
    suspend fun setHideRecentActivity(value: Boolean)

    /**
     * Monitor media discovery view setting
     *
     * @return media discovery view option state as a flow
     */
    fun monitorMediaDiscoveryView(): Flow<Int?>

    /**
     * Set media discovery view
     *
     * @param value
     */
    suspend fun setMediaDiscoveryView(value: Int)

    /**
     * Is camera sync enabled
     *
     * @return
     */
    fun isCameraSyncPreferenceEnabled(): Boolean

    /**
     * Set camera upload
     */
    suspend fun setEnableCameraUpload(enable: Boolean)

    /**
     * Set if photos only or photos and videos
     */
    suspend fun setCameraUploadFileType(syncVideo: Boolean)

    /**
     * Set whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend fun setCamSyncWifi(wifiOnly: Boolean)

    /**
     * Set the camera upload loca path
     */
    suspend fun setCameraUploadLocalPath(path: String?)

    /**
     * Set if camera folder is in external sd card
     */
    suspend fun setCameraFolderExternalSDCard(cameraFolderExternalSDCard: Boolean)

    /**
     * Set conversion charging on size
     */
    suspend fun setChargingOnSize(size: Int)

    /**
     * Get always ask for storage value
     *
     * @return isStorageAskAlways as [Boolean]
     */
    suspend fun getStorageDownloadAskAlways(): Boolean

    /**
     * Set to always ask for storage
     *
     * @param isStorageAskAlways
     */
    suspend fun setStorageAskAlways(isStorageAskAlways: Boolean)

    /**
     * Set the default storage download location
     */
    suspend fun setDefaultStorageDownloadLocation()

    /**
     * Get Storage download location
     *
     * @return storageDownloadLocation as File Path
     */
    suspend fun getStorageDownloadLocation(): String?

    /**
     * Set Storage download location
     *
     * @param storageDownloadLocation
     */
    suspend fun setStorageDownloadLocation(storageDownloadLocation: String)

    /**
     * Set if we want to show copyright notice
     */
    suspend fun setShowCopyright()

    /**
     * Is use https preference set
     *
     * @return true if set
     * @return true if set
     */
    suspend fun isUseHttpsPreferenceEnabled(): Boolean

    /**
     * Set use https preference
     *
     * @param enabled
     */
    suspend fun setUseHttpsPreference(enabled: Boolean)

    /**
     * Gets chat image quality.
     *
     * @return Chat image quality.
     */
    fun getChatImageQuality(): Flow<ChatImageQuality>

    /**
     * Sets chat image quality.
     *
     * @param quality New chat image quality.
     * @return Chat image quality.
     */
    suspend fun setChatImageQuality(quality: ChatImageQuality)

    /**
     * Gets if call notification sounds are enabled or disabled.
     *
     * @return Sound notifications status.
     */
    fun getCallsSoundNotifications(): Flow<CallsSoundNotifications>

    /**
     * Enabling or disabling call notification sounds.
     *
     * @param soundNotifications New Sound notifications status.
     * @return Sound notifications status.
     */
    suspend fun setCallsSoundNotifications(soundNotifications: CallsSoundNotifications)

    /**
     * Set string preference
     *
     * @param key
     * @param value
     */
    suspend fun setStringPreference(key: String?, value: String?)

    /**
     * Set string set preference
     *
     * @param key
     * @param value
     */
    suspend fun setStringSetPreference(key: String?, value: MutableSet<String>?)

    /**
     * Set int preference
     *
     * @param key
     * @param value
     */
    suspend fun setIntPreference(key: String?, value: Int?)

    /**
     * Set long preference
     *
     * @param key
     * @param value
     */
    suspend fun setLongPreference(key: String?, value: Long?)

    /**
     * Set float preference
     *
     * @param key
     * @param value
     */
    suspend fun setFloatPreference(key: String?, value: Float?)

    /**
     * Set boolean preference
     *
     * @param key
     * @param value
     */
    suspend fun setBooleanPreference(key: String?, value: Boolean?)

    /**
     * Monitor string preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorStringPreference(key: String?, defaultValue: String?): Flow<String?>

    /**
     * Monitor string set preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorStringSetPreference(
        key: String?,
        defaultValue: MutableSet<String>?,
    ): Flow<MutableSet<String>?>

    /**
     * Monitor int preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorIntPreference(key: String?, defaultValue: Int): Flow<Int>

    /**
     * Monitor long preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorLongPreference(key: String?, defaultValue: Long): Flow<Long>

    /**
     * Monitor float preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorFloatPreference(key: String?, defaultValue: Float): Flow<Float>

    /**
     * Monitor boolean preference
     *
     * @param key
     * @param defaultValue
     * @return current preference and future updates as a flow
     */
    fun monitorBooleanPreference(key: String?, defaultValue: Boolean): Flow<Boolean>

    /**
     * Get last contact permission dismissed time
     */
    fun getLastContactPermissionDismissedTime(): Flow<Long>

    /**
     * Set last contact permission dismissed time
     *
     * @param time
     */
    suspend fun setLastContactPermissionDismissedTime(time: Long)

    /**
     * Monitor preferred start screen
     *
     * @return flow of start screen preference
     */
    fun monitorPreferredStartScreen(): Flow<StartScreen?>

    /**
     * Set preferred start screen
     *
     * @param screen
     */
    suspend fun setPreferredStartScreen(screen: StartScreen)

    /**
     * Backup time stamps, primary upload folder and secondary folder in share preference after
     * database records being cleaned
     * @param primaryUploadFolderHandle
     * @param secondaryUploadFolderHandle
     * @param camSyncTimeStamp
     * @param camVideoSyncTimeStamp
     * @param secSyncTimeStamp
     * @param secVideoSyncTimeStamp
     */
    suspend fun backupTimestampsAndFolderHandle(
        primaryUploadFolderHandle: Long,
        secondaryUploadFolderHandle: Long,
        camSyncTimeStamp: Long?,
        camVideoSyncTimeStamp: Long?,
        secSyncTimeStamp: Long?,
        secVideoSyncTimeStamp: Long?,
    )


    /**
     * @return [Long] primary handle
     */
    suspend fun getPrimaryHandle(): Long?

    /**
     * @return [Long] secondary handle
     */
    suspend fun getSecondaryHandle(): Long?

    /**
     * @return [String] primary folder photo sync timestamp
     */
    suspend fun getPrimaryFolderPhotoSyncTime(): String?

    /**
     * @return [String] secondary folder photo sync timestamp
     */
    suspend fun getSecondaryFolderPhotoSyncTime(): String?

    /**
     * @return [String] primary folder video sync timestamp
     */
    suspend fun getPrimaryFolderVideoSyncTime(): String?

    /**
     * @return [String] secondary folder video sync timestamp
     */
    suspend fun getSecondaryFolderVideoSyncTime(): String?

    /**
     * Clear Primary Sync Records from Preference
     */
    suspend fun clearPrimaryCameraSyncRecords()

    /**
     * Clear Secondary Sync Records from Preference
     */
    suspend fun clearSecondaryCameraSyncRecords()

    /**
     * Enable file versions option
     *
     * @param enabled
     */
    suspend fun enableFileVersionsOption(enabled: Boolean)

    /**
     * create a default download location file
     * @return File path as [File]
     */
    suspend fun buildDefaultDownloadDir(): File


    /**
     * File Management Preference whether to use Mobile data to preview Hi-res images
     *
     * @return [Boolean]
     */
    suspend fun isMobileDataAllowed(): Boolean

    /**
     * Get Recovery Key
     */
    suspend fun getExportMasterKey(): String?

    /**
     * Set master key exported
     */
    suspend fun setMasterKeyExported()

    /**
     * Check is the master key has been exported
     */
    suspend fun isMasterKeyExported(): Boolean

    /**
     * Check if multi-factor authentication got enabled successfully or not
     * @param pin the valid pin code for multi-factor authentication
     */
    suspend fun enableMultiFactorAuth(pin: String): Boolean

    /**
     * Get the secret code of the account to enable multi-factor authentication
     */
    suspend fun getMultiFactorAuthCode(): String
}
