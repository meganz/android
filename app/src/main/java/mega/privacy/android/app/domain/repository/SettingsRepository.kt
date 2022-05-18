package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.MegaAttributes
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.domain.entity.ChatImageQuality

/**
 * Settings repository - class for handling all calls relating to settings
 *
 */
interface SettingsRepository{
    /**
     * Is sdk logging enabled
     *
     * @return flow that emits true if enabled else false
     */
    fun isSdkLoggingEnabled(): Flow<Boolean>

    /**
     * Set sdk logging enabled
     *
     * @param enabled
     */
    suspend fun setSdkLoggingEnabled(enabled: Boolean)

    /**
     * Is chat logging enabled
     *
     * @return flow that emits true if enabled else false
     */
    fun isChatLoggingEnabled(): Flow<Boolean>

    /**
     * Set chat logging enabled
     *
     * @param enabled
     */
    suspend fun setChatLoggingEnabled(enabled: Boolean)

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

}
