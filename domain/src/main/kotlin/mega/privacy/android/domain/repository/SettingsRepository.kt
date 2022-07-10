package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.ChatImageQuality

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
     * Fetch contact links option
     *
     * @return true if option is enabled, else false
     */
    suspend fun fetchContactLinksOption(): Boolean

    /**
     * Get start screen
     *
     * @return start screen key
     */
    fun getStartScreen(): Int

    /**
     * Should hide recent activity
     *
     * @return true if option is enabled, else false
     */
    fun shouldHideRecentActivity(): Boolean

    /**
     * Set auto accept qr requests
     *
     * @param accept
     * @return true if option is enabled, else false
     */
    suspend fun setAutoAcceptQR(accept: Boolean): Boolean

    /**
     * Monitor start screen
     *
     * @return start screen key changes as a flow
     */
    fun monitorStartScreen(): Flow<Int>

    /**
     * Monitor hide recent activity
     *
     * @return hide recent activity option enabled status as a flow
     */
    fun monitorHideRecentActivity(): Flow<Boolean>

    /**
     * Is camera sync enabled
     *
     * @return
     */
    fun isCameraSyncPreferenceEnabled(): Boolean

    /**
     * Is use https preference set
     *
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
}
