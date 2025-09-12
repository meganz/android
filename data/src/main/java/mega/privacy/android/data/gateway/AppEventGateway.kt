package mega.privacy.android.data.gateway

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.settings.cookie.CookieType

internal interface AppEventGateway {

    /**
     * Monitor cookie settings saved.
     * [Set]  value representing set of enabled cookies of [CookieType]
     */
    val monitorCookieSettings: Flow<Set<CookieType>>


    /**
     * Broadcast enabled cookie settings
     *
     * @param enabledCookieSettings set of enabled cookies of [CookieType]
     */
    suspend fun broadcastCookieSettings(enabledCookieSettings: Set<CookieType>)

    /**
     * Set the status for SMSVerification
     */
    suspend fun setSMSVerificationShown(isShown: Boolean)

    /**
     * Set the status for account security upgrade
     */
    suspend fun setUpgradeSecurity(isSecurityUpgrade: Boolean)

    /**
     * check whether SMS Verification Shown or not
     */
    suspend fun isSMSVerificationShown(): Boolean

    /**
     * Monitor the offline availability of the file
     */
    fun monitorOfflineFileAvailability(): Flow<Long>

    /**
     * Broadcast the offline availability of the file
     * @param nodeHandle the handle of the node
     */
    suspend fun broadcastOfflineFileAvailability(nodeHandle: Long)

    /**
     * Monitor transfer over quota
     */
    fun monitorTransferOverQuota(): Flow<Boolean>

    /**
     * Monitor storage over quota
     */
    fun monitorStorageOverQuota(): Flow<Boolean>

    /**
     * Broadcast transfer over quota
     * @param isCurrentOverQuota true if the overquota is currently received, false otherwise
     *
     */
    suspend fun broadcastTransferOverQuota(isCurrentOverQuota: Boolean)

    /**
     * Broadcast storage over quota
     *
     */
    suspend fun broadcastStorageOverQuota(isCurrentOverQuota: Boolean)

    /**
     * Monitors logout.
     */
    fun monitorLogout(): Flow<Boolean>

    /**
     * Broadcast logout.
     */
    suspend fun broadcastLogout()

    /**
     * Monitor transfer failed
     *
     */
    fun monitorSecurityUpgrade(): Flow<Boolean>

    /**
     * Monitor Finish Activity
     */
    fun monitorFinishActivity(): Flow<Boolean>

    /**
     * Broadcast Finish Activity
     */
    suspend fun broadcastFinishActivity()

    /**
     * Monitors fetch nodes.
     */
    fun monitorFetchNodesFinish(): Flow<Boolean>

    /**
     * Broadcast fetch nodes.
     */
    suspend fun broadcastFetchNodesFinish()

    /**
     * Monitor camera upload folder destination update
     *
     * @return Flow of [CameraUploadsFolderDestinationUpdate]
     */
    fun monitorCameraUploadsFolderDestination(): Flow<CameraUploadsFolderDestinationUpdate>

    /**
     * Broadcast camera upload folder icon update
     *
     * @param data [CameraUploadsFolderDestinationUpdate]
     */
    suspend fun broadcastCameraUploadsFolderDestination(data: CameraUploadsFolderDestinationUpdate)

    /**
     * Broadcast push notification settings
     */
    suspend fun broadcastPushNotificationSettings()

    /**
     * Monitor push notification settings
     */
    fun monitorPushNotificationSettings(): Flow<Boolean>

    /**
     * Monitor account update.
     *
     * @return Flow of [MyAccountUpdate]
     */
    fun monitorMyAccountUpdate(): Flow<MyAccountUpdate>

    /**
     * Broadcast account update.
     *
     * @param data [MyAccountUpdate]
     */
    suspend fun broadcastMyAccountUpdate(data: MyAccountUpdate)

    /**
     * Monitor chat archived.
     *
     * @return Flow [String]
     */
    fun monitorChatArchived(): Flow<String>

    /**
     * Broadcast chat archived.
     *
     * @param chatTitle [String]
     */
    suspend fun broadcastChatArchived(chatTitle: String)

    /**
     * Monitor home badge count.
     *
     * @return Flow of the number of pending actions the current logged in account has.
     */
    fun monitorHomeBadgeCount(): Flow<Int>

    /**
     * Broadcast home badge count.
     *
     * @param badgeCount Number of pending actions the current logged in account has.
     */
    suspend fun broadcastHomeBadgeCount(badgeCount: Int)

    /**
     * Monitor if successfully joined to a chat.
     *
     * @return Flow [Boolean]
     */
    fun monitorJoinedSuccessfully(): Flow<Boolean>

    /**
     * Broadcast if successfully joined to a chat.
     */
    suspend fun broadcastJoinedSuccessfully()

    /**
     * Monitor if waiting for others participants has ended
     *
     * @return Flow [Boolean]
     */
    fun monitorWaitingForOtherParticipantsHasEnded(): Flow<Pair<Long, Boolean>>

    /**
     * Broadcast if waiting for others participants has ended
     */
    suspend fun broadcastWaitingForOtherParticipantsHasEnded(chatId: Long, isEnded: Boolean)

    /**
     * Monitor if should leave a chat.
     *
     * @return Flow [Long] ID of the chat to leave.
     */
    fun monitorLeaveChat(): Flow<Long>

    /**
     * Broadcast that should leave a chat.
     *
     * @param chatId [Long] ID of the chat to leave.
     */
    suspend fun broadcastLeaveChat(chatId: Long)

    /**
     * Broadcast refresh session
     *
     */
    suspend fun broadcastRefreshSession()

    /**
     * Monitor refresh session
     *
     * @return
     */
    fun monitorRefreshSession(): Flow<Unit>

    /**
     * Monitor chat signal presence, monitor if any signal is available
     */
    fun monitorChatSignalPresence(): Flow<Unit>

    /**
     * Broadcast chat signal presence if network signal is available
     */
    suspend fun broadcastChatSignalPresence()

    /**
     * Monitor scheduled meeting canceled.
     *
     * @return Flow [Int]
     */
    fun monitorScheduledMeetingCanceled(): Flow<Int>

    /**
     * Broadcast scheduled meeting canceled.
     *
     * @param messageResId [Int]
     */
    suspend fun broadcastScheduledMeetingCanceled(messageResId: Int)

    /**
     * Monitor CameraUploadSettingsAction.
     *
     * @return Flow [CameraUploadsSettingsAction]
     */
    fun monitorCameraUploadsSettingsActions(): Flow<CameraUploadsSettingsAction>

    /**
     * Broadcast CameraUploadSettingsAction.
     *
     * @param action [CameraUploadsSettingsAction]
     */
    suspend fun broadCastCameraUploadSettingsActions(action: CameraUploadsSettingsAction)


    /**
     * Monitor BackupInfoType.
     *
     * @return Flow [BackupInfoType]
     */
    fun monitorBackupInfoType(): Flow<BackupInfoType>

    /**
     * Broadcast BackupInfoType.
     *
     * @param backupInfoType [BackupInfoType]
     */
    suspend fun broadCastBackupInfoType(backupInfoType: BackupInfoType)

    /**
     * Broadcast business account expired
     */
    suspend fun broadcastBusinessAccountExpired()

    /**
     * Monitor business account expired events
     *
     * @return a flow that emits each time a new business account expired error is received
     */
    fun monitorBusinessAccountExpired(): Flow<Unit>

    /**
     * Broadcast that a specific call has ended.
     *
     * @param chatId    ID of the chat to which the call corresponded.
     */
    suspend fun broadcastCallEnded(chatId: Long)

    /**
     * Monitor that a specific call has opened.
     *
     * @return Flow of Boolean. True, if it's opened. False if not.
     */
    fun monitorCallScreenOpened(): Flow<Boolean>

    /**
     * Broadcast that a specific call has opened.
     *
     * @param isOpened    True, if it's opened. False if not.
     */
    suspend fun broadcastCallScreenOpened(isOpened: Boolean)

    /**
     * Monitor that audio output has changed.
     *
     * @return Flow of AudioDevice.
     */
    fun monitorAudioOutput(): Flow<AudioDevice>

    /**
     * Broadcast that audio output has changed.
     *
     * @param audioDevice   [AudioDevice]
     */
    suspend fun broadcastAudioOutput(audioDevice: AudioDevice)

    /**
     * Monitor that local video has changed due to proximity sensor.
     *
     * @return Flow of Boolean.
     */
    fun monitorLocalVideoChangedDueToProximitySensor(): Flow<Boolean>

    /**
     * Broadcast that local video has changed due to proximity sensor.
     *
     * @param isVideoOn   True, video is on. False, video is off.
     */
    suspend fun broadcastLocalVideoChangedDueToProximitySensor(isVideoOn: Boolean)

    /**
     * Monitor that a specific call has ended.
     *
     * @return Flow of Boolean. ID of the chat to which the call corresponded.
     */
    fun monitorCallEnded(): Flow<Long>

    suspend fun broadcastUpdateUserData()

    fun monitorUpdateUserData(): Flow<Unit>

    /**
     * monitor upgrade dialog closed due to time limit
     *
     */
    fun monitorUpgradeDialogClosed(): Flow<Unit>

    /**
     * broadcast upgrade dialog is closed
     *
     */
    suspend fun broadcastUpgradeDialogClosed()

    /**
     * Monitor misc loaded
     */
    fun monitorMiscLoaded(): Flow<Boolean>

    /**
     * Broadcast misc loaded
     */
    suspend fun broadcastMiscLoaded()

    /**
     * Broadcast misc unloaded
     */
    suspend fun broadcastMiscUnloaded()

    /**
     * Broadcast SSL verification failed
     */
    suspend fun broadcastSslVerificationFailed()

    /**
     * Monitor SSL verification failed
     */
    fun monitorSslVerificationFailed(): Flow<Unit>

    /**
     * Broadcast transfer tag to cancel. Null it no transfer to cancel.
     */
    suspend fun broadcastTransferTagToCancel(transferTag: Int?)

    /**
     * Monitor transfer tag to cancel
     *
     * @return Flow of Int. Null it no transfer to cancel.
     */
    fun monitorTransferTagToCancel(): Flow<Int?>

    /**
     * Set the state indicating if the user session has been logged out from another location.
     */
    suspend fun setLoggedOutFromAnotherLocation(isLoggedOut: Boolean)

    /**
     * Monitor the state indicating if the user session has been logged out from another location.
     *
     * @return Flow of Boolean
     */
    fun monitorLoggedOutFromAnotherLocation(): Flow<Boolean>

    /**
     * Set the state indicating if the current account is an unverified business account.
     */
    suspend fun setIsUnverifiedBusinessAccount(isUnverified: Boolean)

    /**
     * Monitor the state indicating if the current account is an unverified business account.
     *
     * @return Flow of Boolean
     */
    fun monitorIsUnverifiedBusinessAccount(): Flow<Boolean>

    /**
     * Set the state indicating if Google consent has been loaded.
     */
    fun setGoogleConsentLoaded(isLoaded: Boolean)

    /**
     * Monitor the state indicating if Google consent has been loaded.
     *
     * @return Flow of Boolean
     */
    fun monitorGoogleConsentLoaded(): Flow<Boolean>
}
