package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CallsMeetingInvitations
import mega.privacy.android.domain.entity.CallsMeetingReminders
import mega.privacy.android.domain.entity.CallsSoundEnabledState
import mega.privacy.android.domain.entity.ChatImageQuality
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import java.io.File

/**
 * Settings repository - class for handling all calls relating to settings
 *
 */
interface SettingsRepository {

    /**
     * Check if the automatic approval of incoming contact requests using contact links is enabled or disabled
     *
     * If the option has never been set, the error code will be MegaError::API_ENOENT.
     *
     * The associated request type with this request is MegaRequest::TYPE_GET_ATTR_USER
     *
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the value MegaApi::USER_ATTR_CONTACT_LINK_VERIFICATION
     *
     * Valid data in the MegaRequest object received in onRequestFinish when the error code
     * is MegaError::API_OK:
     * - MegaRequest::getText - "0" for disable, "1" for enable
     * - MegaRequest::getFlag - false if disabled, true if enabled
     *
     * @return true if auto-accept is enabled, false otherwise
     */
    suspend fun getContactLinksOption(): Boolean

    /**
     * Enable or disable the automatic approval of incoming contact requests using a
     * contact link
     *
     * The associated request type with this request is MegaRequest::TYPE_SET_ATTR_USER
     *
     * Valid data in the MegaRequest object received on callbacks:
     * - MegaRequest::getParamType - Returns the value
     * MegaApi::USER_ATTR_CONTACT_LINK_VERIFICATION
     *
     * Valid data in the MegaRequest object received in onRequestFinish:
     * - MegaRequest::getText - "0" for disable, "1" for enable
     *
     * @param enable   True to enable the automatic approval of incoming contact requests using a
     *                 contact link
     *
     * @return true if auto-accept is enabled, false otherwise
     */
    suspend fun setContactLinksOption(enable: Boolean): Boolean

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
     * Monitor subfolder media discovery setting
     *
     * @return subfolder media discovery option enabled status as a flow
     */
    fun monitorSubfolderMediaDiscoveryEnabled(): Flow<Boolean?>

    /**
     * Set subfolder media discovery Enabled
     *
     * @param enabled
     */
    suspend fun setSubfolderMediaDiscoveryEnabled(enabled: Boolean)

    /**
     * Monitor show hidden items
     */
    fun monitorShowHiddenItems(): Flow<Boolean>

    /**
     * Set show hidden items
     */
    suspend fun setShowHiddenItems(enabled: Boolean)

    /**
     * Set the default storage download location as current download location
     */
    suspend fun setDefaultDownloadLocation()

    /**
     * Get Storage download location
     *
     * @return storageDownloadLocation as File Path
     */
    suspend fun getDownloadLocation(): String?

    /**
     * Get ask for download location value
     *
     * @return askForDownloadLocation as [Boolean]
     */
    suspend fun isAskForDownloadLocation(): Boolean

    /**
     * Set to ask for storage download location
     *
     * @param askForDownloadLocation
     */
    suspend fun setAskForDownloadLocation(askForDownloadLocation: Boolean)

    /**
     * @return if should ask the user about set the download location as default
     */
    suspend fun isShouldPromptToSaveDestination(): Boolean

    /**
     * Set if should ask the user to save the download location
     */
    suspend fun setShouldPromptToSaveDestination(value: Boolean)

    /**
     * Set download location
     *
     * @param downloadLocation
     */
    suspend fun setDownloadLocation(downloadLocation: String?)

    /**
     * @return ask for confirmation before large downloads preference
     */
    suspend fun isAskBeforeLargeDownloads(): Boolean

    /**
     * Set ask for confirmation before large downloads preference
     */
    suspend fun setAskBeforeLargeDownloads(askForConfirmation: Boolean)

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
     * Gets the current chat video quality.
     *
     * @return Chat video quality.
     */
    suspend fun getChatVideoQualityPreference(): VideoQuality

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
    fun getCallsSoundNotifications(): Flow<CallsSoundEnabledState>

    /**
     * Enabling or disabling call notification sounds.
     *
     * @param soundNotifications New Sound notifications status.
     * @return Sound notifications status.
     */
    suspend fun setCallsSoundNotifications(soundNotifications: CallsSoundEnabledState)

    /**
     * Get calls meeting invitations
     *
     * @return  Meeting invitations status
     */
    fun getCallsMeetingInvitations(): Flow<CallsMeetingInvitations>

    /**
     * Set calls meeting invitations status
     *
     * @param callsMeetingInvitations   New Meeting invitations status
     */
    suspend fun setCallsMeetingInvitations(callsMeetingInvitations: CallsMeetingInvitations)

    /**
     * Get calls meeting reminders
     *
     * @return  Meeting reminders status
     */
    fun getCallsMeetingReminders(): Flow<CallsMeetingReminders>

    /**
     * Set calls meeting reminders status
     *
     * @param callsMeetingReminders New Meeting reminders status
     */
    suspend fun setCallsMeetingReminders(callsMeetingReminders: CallsMeetingReminders)

    /**
     * Get  waiting room reminders
     *
     * @return Waiting room reminders status
     */
    fun getWaitingRoomReminders(): Flow<WaitingRoomReminders>

    /**
     * Set waiting room reminders status
     *
     * @param waitingRoomReminders New Waiting room reminders status
     */
    suspend fun setWaitingRoomReminders(waitingRoomReminders: WaitingRoomReminders)

    /**
     * Get users call limit reminders
     *
     * @return Users call limit reminders status
     */
    fun getUsersCallLimitReminders(): Flow<UsersCallLimitReminders>

    /**
     * Set users call limit reminders status
     *
     * @param usersCallLimitReminders New users call limit reminders status
     */
    suspend fun setUsersCallLimitReminders(usersCallLimitReminders: UsersCallLimitReminders)

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

    /**
     * Set offline warning message visibility
     * @param isVisible the visibility of the view to set
     */
    suspend fun setOfflineWarningMessageVisibility(isVisible: Boolean)

    /**
     * Monitor the offline warning message visibility
     *
     * @return the message visibility state
     */
    fun monitorOfflineWarningMessageVisibility(): Flow<Boolean?>

    /**
     * Handle reset setting when user logout
     */
    suspend fun resetSetting()

    /**
     * Get is first launch
     *
     * @return first launch value, or null if not set
     */
    suspend fun getIsFirstLaunch(): Boolean?

    /**
     * check whether raise to hand feature tooltip is shown or not
     */
    suspend fun isRaiseToHandSuggestionShown(): Boolean?

    /**
     * set raise to hand feature tooltip shown
     */
    suspend fun setRaiseToHandSuggestionShown()

    /**
     * Get file versions option
     *
     * @param forceRefresh
     * @return
     */
    suspend fun getFileVersionsOption(forceRefresh: Boolean): Boolean

    /**
     * Monitor geo tagging status
     *
     * @return true if geo tagging is enabled, false or null otherwise
     */
    fun monitorGeoTaggingStatus(): Flow<Boolean?>

    /**
     * Enable geo tagging
     *
     * @param enabled
     */
    suspend fun enableGeoTagging(enabled: Boolean)

    /**
     * get rubbish bin auto purge period
     *
     * @return number of days
     */
    suspend fun getRubbishBinAutopurgePeriod(): Int

    /**
     * set rubbish bin auto purge period
     *
     * @param days
     */
    suspend fun setRubbishBinAutopurgePeriod(days: Int)

    /**
     * is rubbish bin auto purge enabled
     */
    suspend fun isRubbishBinAutopurgeEnabled(): Boolean

    /**
     * set start screen preference destination
     *
     * @param startScreenDestinationPreference
     */
    suspend fun setStartScreenPreferenceDestination(
        startScreenDestinationPreference: StartScreenDestinationPreference,
    )

    /**
     * Monitor start screen preference destination
     *
     */
    fun monitorStartScreenPreferenceDestination(): Flow<StartScreenDestinationPreference?>

    /**
     * Monitor colored folders onboarding shown preference
     *
     * @return colored folders onboarding shown status as a flow
     */
    fun monitorColoredFoldersOnboardingShown(): Flow<Boolean>

    /**
     * Set colored folders onboarding shown
     *
     * @param shown true if onboarding has been shown, false otherwise
     */
    suspend fun setColoredFoldersOnboardingShown(shown: Boolean)

    /**
     * Monitor enabled home screen widget configuration
     *
     * @return latest widget configuration
     */
    fun monitorHomeScreenWidgetConfiguration(): Flow<List<HomeWidgetConfiguration>>

    /**
     * Update home screen widget configurations
     * This method allows updating the configuration of several widgets at once,
     * inserting missing values and updating existing ones
     *
     * @param configurations List of widget configurations to update
     */
    suspend fun updateHomeScreenWidgetConfiguration(configurations: List<HomeWidgetConfiguration>)

    /**
     * Delete a home screen widget configuration
     *
     * @param widgetIdentifier the identifier of the widget to delete
     */
    suspend fun deleteHomeScreenWidgetConfiguration(widgetIdentifier: String)
}
