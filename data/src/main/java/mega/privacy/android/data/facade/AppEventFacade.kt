package mega.privacy.android.data.facade

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.backup.BackupInfoType
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsSettingsAction
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.entity.transfer.CompletedTransferState
import mega.privacy.android.domain.qualifier.ApplicationScope
import javax.inject.Inject

/**
 * Default implementation of [AppEventGateway]
 */
internal class AppEventFacade @Inject constructor(
    @ApplicationScope private val appScope: CoroutineScope,
) : AppEventGateway {

    private val cameraUploadsFolderDestinationUpdate =
        MutableSharedFlow<CameraUploadsFolderDestinationUpdate>()
    private val _transferOverQuota = MutableStateFlow(false)
    private val _storageOverQuota = MutableStateFlow(false)
    private val _fileAvailableOffline = MutableSharedFlow<Long>()
    private val _cookieSettings = MutableSharedFlow<Set<CookieType>>()
    private val logout = MutableSharedFlow<Boolean>()
    private val fetchNodesFinish = MutableSharedFlow<Boolean>()
    private val pushNotificationSettingsUpdate = MutableSharedFlow<Boolean>()
    private val myAccountUpdate = MutableSharedFlow<MyAccountUpdate>()
    private val chatArchived = MutableSharedFlow<String>()
    private val homeBadgeCount = MutableSharedFlow<Int>()
    private val isJoinedSuccessfullyToChat = MutableSharedFlow<Boolean>()
    private val isWaitingForOtherParticipantsEnded = MutableSharedFlow<Pair<Long, Boolean>>()
    private val leaveChat = MutableSharedFlow<Long>()
    private val chatSignalPresence = MutableSharedFlow<Unit>()
    private val scheduledMeetingCanceled = MutableSharedFlow<Int>()

    private val _isSMSVerificationShownState = MutableStateFlow(false)
    private val _finishActivity = MutableSharedFlow<Boolean>()

    private val updateUpgradeSecurityState = MutableStateFlow(false)
    private val _monitorCompletedTransfer = MutableSharedFlow<CompletedTransferState>()
    private val _monitorRefreshSession = MutableSharedFlow<Unit>()
    private val _monitorBackupInfoType = MutableSharedFlow<BackupInfoType>()
    private val _monitorUpgradeDialogShown = MutableSharedFlow<Unit>()

    private val _monitorCameraUploadsSettingsActions =
        MutableSharedFlow<CameraUploadsSettingsAction>()
    private val _businessAccountExpired = MutableSharedFlow<Unit>()
    private val _sessionLoggedOutFromAnotherLocation = MutableStateFlow(false)
    private val _isUnverifiedBusinessAccount = MutableSharedFlow<Boolean>()
    private val _googleConsentLoaded = MutableStateFlow(false)

    override val monitorCookieSettings: Flow<Set<CookieType>> = _cookieSettings.asSharedFlow()

    override val monitorCompletedTransfer =
        _monitorCompletedTransfer.toSharedFlow(appScope)

    private val callEnded = MutableSharedFlow<Long>()
    private val callScreenOpened = MutableSharedFlow<Boolean>()
    private val audioOutput = MutableSharedFlow<AudioDevice>()
    private val localVideoChangedDueToProximitySensor = MutableSharedFlow<Boolean>()
    private val updateUserData = MutableSharedFlow<Unit>()
    private val miscLoaded = MutableStateFlow(false)
    private val sslVerificationFailed = MutableSharedFlow<Unit>()
    private val transferTagToCancel = MutableSharedFlow<Int?>()

    override suspend fun broadcastCookieSettings(enabledCookieSettings: Set<CookieType>) {
        _cookieSettings.emit(enabledCookieSettings)
    }

    override suspend fun setSMSVerificationShown(isShown: Boolean) {
        _isSMSVerificationShownState.value = isShown
    }

    override suspend fun isSMSVerificationShown(): Boolean = _isSMSVerificationShownState.value

    override fun monitorOfflineFileAvailability(): Flow<Long> =
        _fileAvailableOffline.asSharedFlow()

    override suspend fun broadcastOfflineFileAvailability(nodeHandle: Long) {
        _fileAvailableOffline.emit(nodeHandle)
    }

    override fun monitorTransferOverQuota(): Flow<Boolean> = _transferOverQuota.asSharedFlow()

    override suspend fun broadcastTransferOverQuota(isCurrentOverQuota: Boolean) {
        _transferOverQuota.emit(isCurrentOverQuota)
    }

    override fun monitorStorageOverQuota(): Flow<Boolean> = _storageOverQuota.asSharedFlow()

    override suspend fun broadcastStorageOverQuota(isCurrentOverQuota: Boolean) {
        _storageOverQuota.emit(isCurrentOverQuota)
    }

    override fun monitorLogout(): Flow<Boolean> = logout.asSharedFlow()

    override suspend fun broadcastLogout() = logout.emit(true)

    override fun monitorSecurityUpgrade(): Flow<Boolean> =
        updateUpgradeSecurityState.asStateFlow()

    override suspend fun setUpgradeSecurity(isSecurityUpgrade: Boolean) {
        updateUpgradeSecurityState.value = isSecurityUpgrade
    }

    override fun monitorFinishActivity() = _finishActivity.toSharedFlow(appScope)

    override suspend fun broadcastFinishActivity() = _finishActivity.emit(true)

    override fun monitorFetchNodesFinish() = fetchNodesFinish.asSharedFlow()

    override suspend fun broadcastFetchNodesFinish() = fetchNodesFinish.emit(true)

    override suspend fun broadcastPushNotificationSettings() =
        pushNotificationSettingsUpdate.emit(true)

    override fun monitorPushNotificationSettings() =
        pushNotificationSettingsUpdate.toSharedFlow(appScope)

    override suspend fun broadcastCompletedTransfer(completedTransferState: CompletedTransferState) =
        _monitorCompletedTransfer.emit(completedTransferState)

    override fun monitorMyAccountUpdate(): Flow<MyAccountUpdate> =
        myAccountUpdate.toSharedFlow(appScope)

    override suspend fun broadcastMyAccountUpdate(data: MyAccountUpdate) =
        myAccountUpdate.emit(data)

    override fun monitorCameraUploadsFolderDestination(): Flow<CameraUploadsFolderDestinationUpdate> =
        cameraUploadsFolderDestinationUpdate.toSharedFlow(appScope)

    override suspend fun broadcastCameraUploadsFolderDestination(data: CameraUploadsFolderDestinationUpdate) =
        cameraUploadsFolderDestinationUpdate.emit(data)

    override fun monitorChatArchived(): Flow<String> = chatArchived.toSharedFlow(appScope)

    override suspend fun broadcastChatArchived(chatTitle: String) = chatArchived.emit(chatTitle)

    override fun monitorHomeBadgeCount() =
        homeBadgeCount.shareIn(appScope, started = SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun broadcastHomeBadgeCount(badgeCount: Int) =
        homeBadgeCount.emit(badgeCount)

    override fun monitorJoinedSuccessfully(): Flow<Boolean> =
        isJoinedSuccessfullyToChat.toSharedFlow(appScope)

    override suspend fun broadcastJoinedSuccessfully() =
        isJoinedSuccessfullyToChat.emit(true)

    override fun monitorWaitingForOtherParticipantsHasEnded(): Flow<Pair<Long, Boolean>> =
        isWaitingForOtherParticipantsEnded.toSharedFlow(appScope)

    override suspend fun broadcastWaitingForOtherParticipantsHasEnded(
        chatId: Long,
        isEnded: Boolean,
    ) =
        isWaitingForOtherParticipantsEnded.emit(Pair(chatId, isEnded))

    override fun monitorLeaveChat(): Flow<Long> = leaveChat.toSharedFlow(appScope)

    override suspend fun broadcastLeaveChat(chatId: Long) = leaveChat.emit(chatId)

    override suspend fun broadcastRefreshSession() = _monitorRefreshSession.emit(Unit)

    override fun monitorRefreshSession() = _monitorRefreshSession.asSharedFlow()

    override suspend fun broadcastChatSignalPresence() = chatSignalPresence.emit(Unit)

    override fun monitorChatSignalPresence(): Flow<Unit> = chatSignalPresence.asSharedFlow()

    override fun monitorScheduledMeetingCanceled(): Flow<Int> =
        scheduledMeetingCanceled.toSharedFlow(appScope)

    override suspend fun broadcastScheduledMeetingCanceled(messageResId: Int) =
        scheduledMeetingCanceled.emit(messageResId)

    override fun monitorCameraUploadsSettingsActions() =
        _monitorCameraUploadsSettingsActions.asSharedFlow()

    override suspend fun broadCastCameraUploadSettingsActions(action: CameraUploadsSettingsAction) {
        _monitorCameraUploadsSettingsActions.emit(action)
    }

    override fun monitorBackupInfoType() = _monitorBackupInfoType.asSharedFlow()

    override suspend fun broadCastBackupInfoType(backupInfoType: BackupInfoType) {
        _monitorBackupInfoType.emit(backupInfoType)
    }

    override suspend fun broadcastBusinessAccountExpired() {
        _businessAccountExpired.emit(Unit)
    }

    override fun monitorBusinessAccountExpired() = _businessAccountExpired.asSharedFlow()

    override fun monitorCallEnded() = callEnded.asSharedFlow()

    override suspend fun broadcastCallEnded(callId: Long) = callEnded.emit(callId)

    override fun monitorCallScreenOpened() = callScreenOpened.asSharedFlow()

    override suspend fun broadcastCallScreenOpened(isOpened: Boolean) =
        callScreenOpened.emit(isOpened)

    override fun monitorAudioOutput() = audioOutput.asSharedFlow()

    override suspend fun broadcastAudioOutput(audioDevice: AudioDevice) =
        audioOutput.emit(audioDevice)

    override fun monitorLocalVideoChangedDueToProximitySensor() =
        localVideoChangedDueToProximitySensor.asSharedFlow()

    override suspend fun broadcastLocalVideoChangedDueToProximitySensor(isVideoOn: Boolean) =
        localVideoChangedDueToProximitySensor.emit(isVideoOn)

    override suspend fun broadcastUpdateUserData() {
        updateUserData.emit(Unit)
    }

    override fun monitorUpdateUserData(): Flow<Unit> {
        return updateUserData
    }

    override fun monitorUpgradeDialogClosed(): Flow<Unit> {
        return _monitorUpgradeDialogShown.asSharedFlow()
    }

    override suspend fun broadcastUpgradeDialogClosed() {
        _monitorUpgradeDialogShown.emit(Unit)
    }

    override suspend fun broadcastMiscLoaded() {
        miscLoaded.emit(true)
    }

    override suspend fun broadcastMiscUnloaded() {
        miscLoaded.emit(false)
    }

    override fun monitorMiscLoaded(): Flow<Boolean> {
        return miscLoaded.asStateFlow()
    }

    override suspend fun broadcastSslVerificationFailed() {
        sslVerificationFailed.emit(Unit)
    }

    override fun monitorSslVerificationFailed(): Flow<Unit> {
        return sslVerificationFailed.asSharedFlow()
    }

    override suspend fun broadcastTransferTagToCancel(transferTag: Int?) {
        transferTagToCancel.emit(transferTag)
    }

    override fun monitorTransferTagToCancel(): Flow<Int?> =
        transferTagToCancel.asSharedFlow()

    override suspend fun setLoggedOutFromAnotherLocation(isLoggedOut: Boolean) {
        _sessionLoggedOutFromAnotherLocation.emit(isLoggedOut)
    }

    override fun monitorLoggedOutFromAnotherLocation(): Flow<Boolean> {
        return _sessionLoggedOutFromAnotherLocation.asStateFlow()
    }

    override suspend fun setIsUnverifiedBusinessAccount(isUnverified: Boolean) {
        _isUnverifiedBusinessAccount.emit(isUnverified)
    }

    override fun monitorIsUnverifiedBusinessAccount(): Flow<Boolean> =
        _isUnverifiedBusinessAccount.asSharedFlow()

    override fun setGoogleConsentLoaded(isLoaded: Boolean) {
        _googleConsentLoaded.update { isLoaded }
    }

    override fun monitorGoogleConsentLoaded(): Flow<Boolean> =
        _googleConsentLoaded.asStateFlow()
}

private fun <T> Flow<T>.toSharedFlow(
    scope: CoroutineScope,
) = shareIn(scope, started = SharingStarted.WhileSubscribed())
