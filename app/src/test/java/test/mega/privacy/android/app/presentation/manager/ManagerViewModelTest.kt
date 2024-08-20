package test.mega.privacy.android.app.presentation.manager

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.ShareInfo
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.middlelayer.scanner.ScannerHandler
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.documentscanner.model.DocumentScanningErrorTypeUiItem
import mega.privacy.android.app.presentation.documentscanner.model.HandleScanDocumentResult
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.meeting.chat.model.InfoToShow
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.app.service.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.app.service.scanner.UnexpectedErrorInDocumentScanner
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.IncomingPendingContactRequestAlert
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate.Action
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.UserAlert
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRestartMode
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.environment.DevicePowerConnectionState
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.entity.meeting.UsersCallLimitReminders
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionsResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.MonitorOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.GetFullAccountInfoUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.RenameRecoveryKeyFileUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.GetIncomingContactRequestsUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestUpdatesUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsFolderDestinationUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.link.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.contact.SaveContactByEmailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.file.FilePrepareUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorUpgradeDialogClosedUseCase
import mega.privacy.android.domain.usecase.meeting.SetUsersCallLimitRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.CopyNodesUseCase
import mega.privacy.android.domain.usecase.node.DeleteNodesUseCase
import mega.privacy.android.domain.usecase.node.DisableExportNodesUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesToRubbishUseCase
import mega.privacy.android.domain.usecase.node.MoveNodesUseCase
import mega.privacy.android.domain.usecase.node.RemoveShareUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import mega.privacy.android.domain.usecase.notifications.BroadcastHomeBadgeCountUseCase
import mega.privacy.android.domain.usecase.notifications.GetNumUnreadPromoNotificationsUseCase
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodesUseCase
import mega.privacy.android.domain.usecase.offline.StartOfflineSyncWorkerUseCase
import mega.privacy.android.domain.usecase.photos.mediadiscovery.SendStatisticsMediaDiscoveryUseCase
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.CreateShareKeyUseCase
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.transfers.completed.DeleteOldestCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking
import test.mega.privacy.android.app.InstantExecutorExtension
import test.mega.privacy.android.app.domain.usecase.FakeMonitorBackupFolder
import java.io.File
import java.util.stream.Stream
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class])
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val mutableMonitorUserAlertUpdates = MutableSharedFlow<List<UserAlert>>()
    private val monitorUserAlertUpdates = mutableMonitorUserAlertUpdates.asSharedFlow()
    private lateinit var monitorNodeUpdatesFakeFlow: MutableSharedFlow<NodeUpdate>
    private val monitorContactUpdates = MutableSharedFlow<UserUpdate>()
    private val monitorSecurityUpgradeInApp = MutableStateFlow(false)
    private val getNumUnreadUserAlertsUseCase =
        mock<GetNumUnreadUserAlertsUseCase> { onBlocking { invoke() }.thenReturn(0) }
    private val getNumUnreadPromoNotificationsUseCase =
        mock<GetNumUnreadPromoNotificationsUseCase>()
    private val monitorContactRequestUpdatesUseCase = mock<MonitorContactRequestUpdatesUseCase> {
        on { invoke() }.thenReturn(emptyFlow())
    }

    private val initialIsFirsLoginValue = true
    private val sendStatisticsMediaDiscoveryUseCase = mock<SendStatisticsMediaDiscoveryUseCase>()
    private val savedStateHandle = SavedStateHandle(
        mapOf(
            ManagerViewModel.IS_FIRST_LOGIN_KEY to initialIsFirsLoginValue
        )
    )
    private val monitorStorageState = mock<MonitorStorageStateEventUseCase> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(
                StorageStateEvent(
                    handle = 0L,
                    eventString = "",
                    number = 0L,
                    text = "",
                    type = EventType.Storage,
                    storageState = StorageState.Unknown,
                )
            )
        )
    }
    private val getPrimarySyncHandleUseCase = mock<GetPrimarySyncHandleUseCase>()
    private val getSecondarySyncHandleUseCase = mock<GetSecondarySyncHandleUseCase>()
    private val monitorCameraUploadsFolderDestinationUpdateUseCase =
        mock<MonitorCameraUploadsFolderDestinationUseCase> {
            onBlocking { invoke() }.thenReturn(
                flowOf(
                    CameraUploadsFolderDestinationUpdate(6L, CameraUploadFolderType.Primary),
                    CameraUploadsFolderDestinationUpdate(9L, CameraUploadFolderType.Secondary)
                )
            )
        }
    private val getCloudSortOrder =
        mock<GetCloudSortOrder> { onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC) }
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorSyncStalledIssuesUseCase = mock<MonitorSyncStalledIssuesUseCase> {
        onBlocking { invoke() }.thenReturn(emptyFlow())
    }
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val shareDataList = listOf(
        ShareData(
            user = "user",
            nodeHandle = 8766L,
            access = AccessPermission.READ,
            timeStamp = 987654678L,
            isPending = true,
            isVerified = false,
            count = 0
        ),
        ShareData(
            user = "user",
            nodeHandle = 8766L,
            access = AccessPermission.READ,
            timeStamp = 987654678L,
            isPending = true,
            isVerified = false,
            count = 0
        )
    )
    private val getUnverifiedOutgoingShares = mock<GetUnverifiedOutgoingShares> {
        onBlocking {
            invoke(
                any()
            )
        }.thenReturn(
            shareDataList
        )
    }
    private val getUnverifiedIncomingShares = mock<GetUnverifiedIncomingShares> {
        onBlocking {
            invoke(
                any()
            )
        }.thenReturn(shareDataList)
    }
    private val initialFinishActivityValue = false
    private val monitorFinishActivity = mock<MonitorFinishActivityUseCase> {
        onBlocking { invoke() }.thenReturn(
            flowOf(
                initialFinishActivityValue,
                !initialFinishActivityValue
            )
        )
    }

    private val monitorPushNotificationSettingsUpdate =
        mock<MonitorUpdatePushNotificationSettingsUseCase> {
            onBlocking { invoke() }.thenReturn(flowOf(true))
        }
    private val requireTwoFactorAuthenticationUseCase =
        mock<RequireTwoFactorAuthenticationUseCase>()
    private val setCopyLatestTargetPathUseCase = mock<SetCopyLatestTargetPathUseCase>()
    private val setMoveLatestTargetPathUseCase = mock<SetMoveLatestTargetPathUseCase>()
    private val monitorUserUpdates = mock<MonitorUserUpdates> {
        onBlocking { invoke() }.thenReturn(emptyFlow())
    }
    private var monitorMyAccountUpdateFakeFlow = MutableSharedFlow<MyAccountUpdate>()
    private val establishCameraUploadsSyncHandlesUseCase =
        mock<EstablishCameraUploadsSyncHandlesUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()
    private val saveContactByEmailUseCase = mock<SaveContactByEmailUseCase>()
    private val createShareKeyUseCase = mock<CreateShareKeyUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val deleteOldestCompletedTransfersUseCase =
        mock<DeleteOldestCompletedTransfersUseCase>()
    private val monitorOfflineNodeAvailabilityUseCase =
        mock<MonitorOfflineFileAvailabilityUseCase>()
    private val getIncomingContactRequestUseCase = mock<GetIncomingContactRequestsUseCase>()
    private val monitorChatArchivedUseCase = mock<MonitorChatArchivedUseCase> {
        onBlocking { invoke() }.thenReturn(flowOf("Chat Title"))
    }
    private val getFullAccountInfoUseCase = mock<GetFullAccountInfoUseCase>()
    private val restoreNodesUseCase = mock<RestoreNodesUseCase>()
    private val checkNodesNameCollisionUseCase = mock<CheckNodesNameCollisionUseCase>()
    private val monitorBackupFolder = FakeMonitorBackupFolder()
    private val moveNodesToRubbishUseCase = mock<MoveNodesToRubbishUseCase>()
    private val deleteNodesUseCase = mock<DeleteNodesUseCase>()
    private val removeOfflineNodesUseCase = mock<RemoveOfflineNodesUseCase>()
    private val moveNodesUseCase = mock<MoveNodesUseCase>()
    private val copyNodesUseCase = mock<CopyNodesUseCase>()
    private val renameRecoveryKeyFileUseCase = mock<RenameRecoveryKeyFileUseCase>()
    private val removeShareUseCase: RemoveShareUseCase = mock()
    private val removeShareResultMapper: RemoveShareResultMapper = mock()
    private val getNumUnreadChatsUseCase: GetNumUnreadChatsUseCase = mock()
    private val disableExportNodesUseCase: DisableExportNodesUseCase = mock()
    private val removePublicLinkResultMapper: RemovePublicLinkResultMapper = mock()
    private val dismissPsaUseCase = mock<DismissPsaUseCase>()
    private val rootNodeUseCase = mock<GetRootNodeUseCase>()
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase = mock()
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private val setUsersCallLimitRemindersUseCase: SetUsersCallLimitRemindersUseCase = mock()
    private val getUsersCallLimitRemindersUseCase: GetUsersCallLimitRemindersUseCase = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val chatManagement: ChatManagement = mock()
    private val passcodeManagement: PasscodeManagement = mock()
    private lateinit var monitorSyncsUseCaseFakeFlow: MutableSharedFlow<List<FolderPair>>
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase = mock()
    private val hangChatCallUseCase: HangChatCallUseCase = mock()
    private val startOfflineSyncWorkerUseCase: StartOfflineSyncWorkerUseCase = mock()
    private val scannerHandler: ScannerHandler = mock()
    private val fakeCallUpdatesFlow = MutableSharedFlow<ChatCall>()
    private var monitorDevicePowerConnectionFakeFlow =
        MutableSharedFlow<DevicePowerConnectionState>()

    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase = mock {
        onBlocking { invoke() }.thenReturn(fakeCallUpdatesFlow)
    }
    private val broadcastHomeBadgeCountUseCase = mock<BroadcastHomeBadgeCountUseCase>()

    private val monitorUpgradeDialogClosedFlow = MutableSharedFlow<Unit>()

    private val monitorUpgradeDialogClosedUseCase: MonitorUpgradeDialogClosedUseCase = mock {
        onBlocking { invoke() }.thenReturn(monitorUpgradeDialogClosedFlow)
    }

    private val filePrepareUseCase = mock<FilePrepareUseCase>()


    private fun initViewModel() {
        underTest = ManagerViewModel(
            monitorNodeUpdatesUseCase = mock {
                on { invoke() }.thenReturn(monitorNodeUpdatesFakeFlow)
            },
            monitorContactUpdates = { monitorContactUpdates },
            monitorUserAlertUpdates = { monitorUserAlertUpdates },
            monitorContactRequestUpdatesUseCase = monitorContactRequestUpdatesUseCase,
            getNumUnreadUserAlertsUseCase = getNumUnreadUserAlertsUseCase,
            getNumUnreadPromoNotificationsUseCase = getNumUnreadPromoNotificationsUseCase,
            sendStatisticsMediaDiscoveryUseCase = sendStatisticsMediaDiscoveryUseCase,
            savedStateHandle = savedStateHandle,
            monitorStorageStateEventUseCase = monitorStorageState,
            monitorCameraUploadsFolderDestinationUseCase = monitorCameraUploadsFolderDestinationUpdateUseCase,
            getCloudSortOrder = getCloudSortOrder,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            getExtendedAccountDetail = mock(),
            getFullAccountInfoUseCase = getFullAccountInfoUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getUnverifiedIncomingShares = getUnverifiedIncomingShares,
            getUnverifiedOutgoingShares = getUnverifiedOutgoingShares,
            monitorFinishActivityUseCase = monitorFinishActivity,
            requireTwoFactorAuthenticationUseCase = requireTwoFactorAuthenticationUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            monitorSecurityUpgradeInApp = { monitorSecurityUpgradeInApp },
            monitorUserUpdates = monitorUserUpdates,
            establishCameraUploadsSyncHandlesUseCase = establishCameraUploadsSyncHandlesUseCase,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            saveContactByEmailUseCase = saveContactByEmailUseCase,
            createShareKeyUseCase = createShareKeyUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            deleteOldestCompletedTransfersUseCase = deleteOldestCompletedTransfersUseCase,
            getIncomingContactRequestsUseCase = getIncomingContactRequestUseCase,
            monitorMyAccountUpdateUseCase = mock {
                on { invoke() }.thenReturn(monitorMyAccountUpdateFakeFlow)
            },
            monitorUpdatePushNotificationSettingsUseCase = monitorPushNotificationSettingsUpdate,
            monitorOfflineNodeAvailabilityUseCase = monitorOfflineNodeAvailabilityUseCase,
            monitorChatArchivedUseCase = monitorChatArchivedUseCase,
            restoreNodesUseCase = restoreNodesUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            monitorBackupFolder = monitorBackupFolder,
            moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
            deleteNodesUseCase = deleteNodesUseCase,
            removeOfflineNodesUseCase = removeOfflineNodesUseCase,
            moveNodesUseCase = moveNodesUseCase,
            copyNodesUseCase = copyNodesUseCase,
            renameRecoveryKeyFileUseCase = renameRecoveryKeyFileUseCase,
            removeShareUseCase = removeShareUseCase,
            removeShareResultMapper = removeShareResultMapper,
            getNumUnreadChatsUseCase = getNumUnreadChatsUseCase,
            disableExportNodesUseCase = disableExportNodesUseCase,
            removePublicLinkResultMapper = removePublicLinkResultMapper,
            dismissPsaUseCase = dismissPsaUseCase,
            getRootNodeUseCase = rootNodeUseCase,
            getChatLinkContentUseCase = getChatLinkContentUseCase,
            getScheduledMeetingByChatUseCase = getScheduledMeetingByChatUseCase,
            getChatCallUseCase = getChatCallUseCase,
            startMeetingInWaitingRoomChatUseCase = startMeetingInWaitingRoomChatUseCase,
            answerChatCallUseCase = answerChatCallUseCase,
            setChatVideoInDeviceUseCase = setChatVideoInDeviceUseCase,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            chatManagement = chatManagement,
            passcodeManagement = passcodeManagement,
            monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
            monitorSyncsUseCase = mock {
                on { invoke() }.thenReturn(monitorSyncsUseCaseFakeFlow)
            },
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            hangChatCallUseCase = hangChatCallUseCase,
            monitorChatCallUpdatesUseCase = monitorChatCallUpdatesUseCase,
            getUsersCallLimitRemindersUseCase = getUsersCallLimitRemindersUseCase,
            setUsersCallLimitRemindersUseCase = setUsersCallLimitRemindersUseCase,
            broadcastHomeBadgeCountUseCase = broadcastHomeBadgeCountUseCase,
            monitorUpgradeDialogClosedUseCase = monitorUpgradeDialogClosedUseCase,
            monitorDevicePowerConnectionStateUseCase = mock {
                on { invoke() }.thenReturn(monitorDevicePowerConnectionFakeFlow)
            },
            startOfflineSyncWorkerUseCase = startOfflineSyncWorkerUseCase,
            filePrepareUseCase = filePrepareUseCase,
            scannerHandler = scannerHandler,
        )
    }

    @BeforeEach
    fun reset() {
        reset(
            sendStatisticsMediaDiscoveryUseCase,
            isConnectedToInternetUseCase,
            getFullAccountInfoUseCase,
            requireTwoFactorAuthenticationUseCase,
            setCopyLatestTargetPathUseCase,
            setMoveLatestTargetPathUseCase,
            establishCameraUploadsSyncHandlesUseCase,
            startCameraUploadUseCase,
            stopCameraUploadsUseCase,
            saveContactByEmailUseCase,
            createShareKeyUseCase,
            deleteOldestCompletedTransfersUseCase,
            restoreNodesUseCase,
            checkNodesNameCollisionUseCase,
            moveNodesToRubbishUseCase,
            deleteNodesUseCase,
            moveNodesUseCase,
            copyNodesUseCase,
            renameRecoveryKeyFileUseCase,
            removeShareUseCase,
            removeShareResultMapper,
            getNumUnreadChatsUseCase,
            disableExportNodesUseCase,
            removePublicLinkResultMapper,
            dismissPsaUseCase,
            rootNodeUseCase,
            getChatLinkContentUseCase,
            getScheduledMeetingByChatUseCase,
            getChatCallUseCase,
            startMeetingInWaitingRoomChatUseCase,
            answerChatCallUseCase,
            setChatVideoInDeviceUseCase,
            rtcAudioManagerGateway,
            chatManagement,
            passcodeManagement,
            hangChatCallUseCase,
            monitorChatArchivedUseCase,
            monitorPushNotificationSettingsUpdate,
            getUsersCallLimitRemindersUseCase,
            setUsersCallLimitRemindersUseCase,
            broadcastHomeBadgeCountUseCase,
            monitorUpgradeDialogClosedUseCase,
            monitorContactRequestUpdatesUseCase,
            startOfflineSyncWorkerUseCase,
            filePrepareUseCase,
            scannerHandler,
        )
        wheneverBlocking { getCloudSortOrder() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC)
        whenever(getUsersCallLimitRemindersUseCase()).thenReturn(emptyFlow())
        wheneverBlocking { getFeatureFlagValueUseCase(any()) }.thenReturn(true)
        whenever(monitorUserUpdates()).thenReturn(emptyFlow())
        whenever(monitorChatArchivedUseCase()).thenReturn(flowOf("Chat Title"))
        whenever(monitorPushNotificationSettingsUpdate()).thenReturn(flowOf(true))
        wheneverBlocking { getPrimarySyncHandleUseCase() }.thenReturn(0L)
        wheneverBlocking { getSecondarySyncHandleUseCase() }.thenReturn(0L)
        wheneverBlocking { getNumUnreadUserAlertsUseCase() }.thenReturn(0)
        wheneverBlocking { getNumUnreadPromoNotificationsUseCase() }.thenReturn(0)
        wheneverBlocking { getIncomingContactRequestUseCase() }.thenReturn(emptyList())
        whenever(monitorContactRequestUpdatesUseCase()).thenReturn(emptyFlow())
        monitorNodeUpdatesFakeFlow = MutableSharedFlow()
        monitorSyncsUseCaseFakeFlow = MutableSharedFlow()
        monitorMyAccountUpdateFakeFlow = MutableSharedFlow()
        monitorDevicePowerConnectionFakeFlow = MutableSharedFlow()
        initViewModel()
    }

    @Test
    fun `test that the option returned by getUsersCallLimitRemindersUseCase is set as enabled`() =
        runTest {
            whenever(getUsersCallLimitRemindersUseCase()).thenReturn(flowOf(UsersCallLimitReminders.Enabled))
            underTest.state.map { it.usersCallLimitReminders }.distinctUntilChanged().test {
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(UsersCallLimitReminders.Enabled)
            }
        }

    @Test
    fun `test that setUsersCallLimitRemindersUseCase calls the set use case with the correct value`() =
        runTest {
            underTest.setUsersCallLimitReminder(false)
            testScheduler.advanceUntilIdle()
            verify(setUsersCallLimitRemindersUseCase).invoke(UsersCallLimitReminders.Disabled)
        }

    @Test
    fun `test that the user root backups folder node id is set when an update is received`() =
        runTest {
            testScheduler.advanceUntilIdle()
            underTest.state.map { it.userRootBackupsFolderHandle }.distinctUntilChanged()
                .test {
                    val newValue = NodeId(123456L)
                    assertThat(awaitItem()).isEqualTo(NodeId(-1L))
                    monitorBackupFolder.emit(Result.success(newValue))
                    testScheduler.advanceUntilIdle()
                    assertThat(awaitItem()).isEqualTo(newValue)
                }
        }

    @Test
    fun `test that is first navigation level is updated if new value provided`() = runTest {
        underTest.state.map { it.isFirstNavigationLevel }.distinctUntilChanged()
            .test {
                val newValue = false
                assertThat(awaitItem()).isEqualTo(true)
                underTest.setIsFirstNavigationLevel(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that shares tab is updated if new value provided`() = runTest {
        underTest.state.map { it.sharesTab }.distinctUntilChanged()
            .test {
                val newValue = SharesTab.OUTGOING_TAB
                assertThat(awaitItem()).isEqualTo(SharesTab.INCOMING_TAB)
                underTest.setSharesTab(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that saved initial state values are returned`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map {
            it.isFirstLogin
        }.test {
            assertThat(awaitItem()).isEqualTo(initialIsFirsLoginValue)
        }
    }

    @Test
    fun `test that is first login is updated if new boolean is provided`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.isFirstLogin }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isEqualTo(initialIsFirsLoginValue)
                underTest.setIsFirstLogin(!initialIsFirsLoginValue)
                assertThat(awaitItem()).isEqualTo(!initialIsFirsLoginValue)
            }
    }

    @Test
    fun `test that get order returns cloud sort order`() = runTest {

        val expected = SortOrder.ORDER_MODIFICATION_DESC
        whenever(getCloudSortOrder()).thenReturn(expected)
        assertThat(underTest.getOrder()).isEqualTo(expected)
    }

    @Test
    fun `test that updateToolbarTitle is true when a node update occurs`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.nodeUpdateReceived }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that updateToolbarTitle is set when calling setUpdateToolbar`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.nodeUpdateReceived }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            monitorNodeUpdatesFakeFlow.emit(NodeUpdate(emptyMap()))
            testScheduler.advanceUntilIdle()
            assertThat(awaitItem()).isTrue()
            underTest.nodeUpdateHandled()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that pending actions count matches incoming and outgoing shares size`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.pendingActionsCount }.distinctUntilChanged().test {
            assertThat(awaitItem()).isEqualTo(shareDataList.size * 2)
        }
    }

    @Test
    fun `test that monitor finish activity event value matches the initial value returned by use case`() =
        runTest {
            underTest.monitorFinishActivityEvent.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(initialFinishActivityValue)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitor finish activity events are returned`() =
        runTest {
            underTest.monitorFinishActivityEvent.distinctUntilChanged().test {
                whenever(monitorFinishActivity()).thenReturn(flowOf(!initialFinishActivityValue))
                assertThat(awaitItem()).isEqualTo(initialFinishActivityValue)
                monitorFinishActivity()
                assertThat(awaitItem()).isEqualTo(!initialFinishActivityValue)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that monitor my account update events are returned`() =
        runTest {
            underTest.monitorMyAccountUpdateEvent.test {
                monitorMyAccountUpdateFakeFlow.emit(
                    MyAccountUpdate(Action.STORAGE_STATE_CHANGED, StorageState.Green),
                )
                assertThat(awaitItem().action).isEqualTo(Action.STORAGE_STATE_CHANGED)

                monitorMyAccountUpdateFakeFlow.emit(
                    MyAccountUpdate(Action.UPDATE_ACCOUNT_DETAILS, null)
                )
                assertThat(awaitItem().action).isEqualTo(Action.UPDATE_ACCOUNT_DETAILS)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test when check2FADialog returns false show2FADialog flow updated to false`() = runTest {
        whenever(
            requireTwoFactorAuthenticationUseCase(
                newAccount = false,
                firstLogin = true
            )
        ).thenReturn(false)
        underTest.checkToShow2FADialog(newAccount = false, firstLogin = true)
        underTest.state.map { it }.distinctUntilChanged().test {
            assertThat(awaitItem().show2FADialog).isFalse()
        }
    }

    @Test
    fun `test when check2FADialog returns true show2FADialog flow updated to true`() = runTest {
        whenever(
            requireTwoFactorAuthenticationUseCase(
                newAccount = false,
                firstLogin = true
            )
        ).thenReturn(true)
        underTest.checkToShow2FADialog(newAccount = false, firstLogin = true)
        testScheduler.advanceUntilIdle()
        underTest.state.map { it }.distinctUntilChanged().test {
            assertThat(awaitItem().show2FADialog).isTrue()
        }
    }

    @Test
    fun `test that the camera uploads sync handles are established when receiving an update to change the camera uploads folders`() =
        runTest {
            val userUpdates = listOf(
                UserChanges.Birthday,
                UserChanges.CameraUploadsFolder,
                UserChanges.Country,
            )

            testScheduler.advanceUntilIdle()
            whenever(monitorUserUpdates()).thenReturn(userUpdates.asFlow())
            initViewModel()
            testScheduler.advanceUntilIdle()

            verify(establishCameraUploadsSyncHandlesUseCase).invoke()
        }

    @Test
    fun `test that when push notification settings is updated state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorPushNotificationSettingsUpdate).invoke()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.isPushNotificationSettingsUpdatedEvent).isTrue()
            }
        }

    @Test
    fun `test that when stopAndDisableCameraUploads is called, stopCameraUploadUseCase is called`() =
        runTest {
            underTest.stopAndDisableCameraUploads()
            testScheduler.advanceUntilIdle()
            verify(stopCameraUploadsUseCase).invoke(CameraUploadsRestartMode.StopAndDisable)
        }

    @ParameterizedTest
    @EnumSource(
        value = StorageState::class,
        names = ["Green", "Orange"],
        mode = EnumSource.Mode.INCLUDE,
    )
    fun `test that camera uploads is started when an account event indicating enough storage space is received`(
        storageState: StorageState,
    ) = runTest {
        monitorMyAccountUpdateFakeFlow.emit(
            MyAccountUpdate(Action.STORAGE_STATE_CHANGED, storageState)
        )
        advanceUntilIdle()
        verify(startCameraUploadUseCase).invoke()
    }

    @ParameterizedTest
    @EnumSource(
        value = StorageState::class,
        names = ["Green", "Orange"],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `test that camera uploads is not started when an account event indicating not enough storage space is received`(
        storageState: StorageState,
    ) = runTest {
        monitorMyAccountUpdateFakeFlow.emit(
            MyAccountUpdate(Action.STORAGE_STATE_CHANGED, storageState)
        )
        advanceUntilIdle()
        verify(startCameraUploadUseCase, never()).invoke()
    }

    @Test
    fun `test that monitor camera upload folder icon update events are returned`() =
        runTest {
            underTest.monitorCameraUploadFolderIconUpdateEvent.distinctUntilChanged().test {
                assertThat(awaitItem()).isEqualTo(
                    CameraUploadsFolderDestinationUpdate(
                        6L,
                        CameraUploadFolderType.Primary
                    )
                )
                assertThat(awaitItem()).isEqualTo(
                    CameraUploadsFolderDestinationUpdate(
                        9L,
                        CameraUploadFolderType.Secondary
                    )
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that deletion of oldest completed transfers is triggered on init`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(deleteOldestCompletedTransfersUseCase).invoke()
        }

    @Test
    fun `test that incomingContactRequests getting update when there is contact request event`() =
        runTest {
            underTest.incomingContactRequests.test {
                assertThat(awaitItem()).isEmpty()
            }
            val contactRequests = listOf(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "",
                    sourceMessage = null,
                    targetEmail = "",
                    creationTime = 1L,
                    modificationTime = 1L,
                    status = ContactRequestStatus.Unresolved,
                    isOutgoing = false,
                    isAutoAccepted = false,
                )
            )
            whenever(getIncomingContactRequestUseCase()).thenReturn(contactRequests)
            whenever(monitorContactRequestUpdatesUseCase()).thenReturn(flowOf(contactRequests))

            initViewModel()
            testScheduler.advanceUntilIdle()
            underTest.incomingContactRequests.test {
                assertThat(awaitItem()).isEqualTo(contactRequests)
            }
        }

    @Test
    fun `test that incomingContactRequests getting update when monitorGlobalUpdates emit`() =
        runTest {
            underTest.incomingContactRequests.test {
                assertThat(awaitItem()).isEmpty()
            }
            val contactRequests = listOf(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "",
                    sourceMessage = null,
                    targetEmail = "",
                    creationTime = 1L,
                    modificationTime = 1L,
                    status = ContactRequestStatus.Unresolved,
                    isOutgoing = false,
                    isAutoAccepted = false,
                )
            )
            whenever(getIncomingContactRequestUseCase()).thenReturn(contactRequests)
            whenever(getNumUnreadUserAlertsUseCase()).thenReturn(3)
            whenever(getNumUnreadPromoNotificationsUseCase()).thenReturn(1)
            mutableMonitorUserAlertUpdates.emit(
                listOf(
                    IncomingPendingContactRequestAlert(
                        id = 1,
                        seen = false,
                        createdTime = 100,
                        isOwnChange = false,
                        contact = Contact(
                            userId = 2,
                            hasPendingRequest = true,
                            email = "test@mega.co.nz"
                        )
                    )
                )
            )
            testScheduler.advanceUntilIdle()
            underTest.incomingContactRequests.test {
                assertThat(awaitItem()).isEqualTo(contactRequests)
            }
        }

    @Test
    fun `test that saveContactByEmailUseCase invoke when there is incoming contact request event`() =
        runTest {
            val contactRequests = listOf(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "sourceEmail@mega.co.nz",
                    sourceMessage = null,
                    targetEmail = "targetEmail@mega.co.nz",
                    creationTime = 1L,
                    modificationTime = 1L,
                    status = ContactRequestStatus.Accepted,
                    isOutgoing = false,
                    isAutoAccepted = false,
                )
            )
            whenever(getIncomingContactRequestUseCase()).thenReturn(contactRequests)
            whenever(getNumUnreadUserAlertsUseCase()).thenReturn(3)
            whenever(getNumUnreadPromoNotificationsUseCase()).thenReturn(1)
            whenever(monitorContactRequestUpdatesUseCase()).thenReturn(flowOf(contactRequests))
            initViewModel()
            advanceUntilIdle()
            verify(saveContactByEmailUseCase).invoke("sourceEmail@mega.co.nz")
        }

    @Test
    fun `test that saveContactByEmailUseCase invoke when there is isOutgoing contact request event`() =
        runTest {
            val contactRequests = listOf(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "sourceEmail@mega.co.nz",
                    sourceMessage = null,
                    targetEmail = "targetEmail@mega.co.nz",
                    creationTime = 1L,
                    modificationTime = 1L,
                    status = ContactRequestStatus.Accepted,
                    isOutgoing = true,
                    isAutoAccepted = false,
                )
            )
            whenever(getIncomingContactRequestUseCase()).thenReturn(contactRequests)
            whenever(getNumUnreadUserAlertsUseCase()).thenReturn(3)
            whenever(getNumUnreadPromoNotificationsUseCase()).thenReturn(1)
            whenever(monitorContactRequestUpdatesUseCase()).thenReturn(flowOf(contactRequests))
            initViewModel()
            advanceUntilIdle()
            verify(saveContactByEmailUseCase).invoke("targetEmail@mega.co.nz")
        }

    @Test
    fun `test that legacyNumUnreadUserAlerts update when there is accepted contact request event`() =
        runTest {
            val expectedPromoNotificationsCount = 1
            val expectedUserAlertsCount = 3
            val expectedTotalCount = expectedPromoNotificationsCount + expectedUserAlertsCount
            advanceUntilIdle()
            val contactRequests = listOf(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "",
                    sourceMessage = null,
                    targetEmail = "",
                    creationTime = 1L,
                    modificationTime = 1L,
                    status = ContactRequestStatus.Accepted,
                    isOutgoing = false,
                    isAutoAccepted = false,
                )
            )
            whenever(getIncomingContactRequestUseCase()).thenReturn(contactRequests)
            whenever(getNumUnreadUserAlertsUseCase()).thenReturn(expectedUserAlertsCount)
            whenever(getNumUnreadPromoNotificationsUseCase()).thenReturn(
                expectedPromoNotificationsCount
            )
            whenever(monitorContactRequestUpdatesUseCase()).thenReturn(flowOf(contactRequests))
            initViewModel()
            advanceUntilIdle()
            verify(broadcastHomeBadgeCountUseCase).invoke(expectedTotalCount + contactRequests.size)
            assertThat(underTest.onGetNumUnreadUserAlerts().test().value().second).isEqualTo(
                expectedTotalCount
            )
        }

    @Test
    fun `test that numUnreadUserAlerts update when there is accepted contact request event`() =
        runTest {
            val expectedPromoNotificationsCount = 1
            val expectedUserAlertsCount = 3
            val expectedTotalCount = expectedPromoNotificationsCount + expectedUserAlertsCount
            val requests = MutableStateFlow<List<ContactRequest>>(emptyList())
            whenever(monitorContactRequestUpdatesUseCase()).thenReturn(requests.filterNot { it.isEmpty() })
            initViewModel()
            advanceUntilIdle()
            assertThat(underTest.numUnreadUserAlerts.value.second).isEqualTo(0)

            val contactRequests = listOf(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "",
                    sourceMessage = null,
                    targetEmail = "",
                    creationTime = 1L,
                    modificationTime = 1L,
                    status = ContactRequestStatus.Accepted,
                    isOutgoing = false,
                    isAutoAccepted = false,
                )
            )
            whenever(getIncomingContactRequestUseCase()).thenReturn(contactRequests)
            whenever(getNumUnreadUserAlertsUseCase()).thenReturn(expectedUserAlertsCount)
            whenever(getNumUnreadPromoNotificationsUseCase()).thenReturn(
                expectedPromoNotificationsCount
            )

            requests.emit(contactRequests)
            advanceUntilIdle()
            verify(broadcastHomeBadgeCountUseCase).invoke(expectedTotalCount + contactRequests.size)
            assertThat(underTest.numUnreadUserAlerts.value.second).isEqualTo(expectedTotalCount)
        }

    @Test
    fun `test that an exception from get full account info is not propagated`() = runTest {
        whenever(getFullAccountInfoUseCase()).thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            askForFullAccountInfo()
            state.test {
                assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
            }
        }
    }

    @Test
    fun `test that an exception from require 2FA is not propagated`() = runTest {
        whenever(requireTwoFactorAuthenticationUseCase(newAccount = false, firstLogin = false))
            .thenAnswer { throw MegaException(1, "It's broken") }

        with(underTest) {
            checkToShow2FADialog(newAccount = false, firstLogin = false)
            state.map { it.show2FADialog }.test {
                assertFalse(awaitItem())
                assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
            }
        }
    }

    @Test
    fun `test that when a chat is archived state is updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorChatArchivedUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.titleChatArchivedEvent).isNotNull()
            }
        }

    @Test
    fun `test that when onChatArchivedEventConsumed is called then state is also updated`() =
        runTest {
            testScheduler.advanceUntilIdle()
            verify(monitorChatArchivedUseCase).invoke()
            underTest.state.test {
                val state = awaitItem()
                assertThat(state.titleChatArchivedEvent).isNotNull()
                underTest.onChatArchivedEventConsumed()
                val updatedState = awaitItem()
                assertThat(updatedState.titleChatArchivedEvent).isNull()
            }
        }

    @Test
    fun `test that restoreNodeResult updated when calling restoreNodes successfully`() = runTest {
        testScheduler.advanceUntilIdle()
        val node = mock<MegaNode> {
            on { handle }.thenReturn(1L)
            on { restoreHandle }.thenReturn(100L)
        }
        val result = mock<SingleNodeRestoreResult>()
        whenever(restoreNodesUseCase(mapOf(node.handle to node.restoreHandle))).thenReturn(result)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.restoreNodeResult).isNull()
            underTest.restoreNodes(mapOf(node.handle to node.restoreHandle))
            val updatedState = awaitItem()
            assertThat(updatedState.restoreNodeResult).isNotNull()
            assertThat(updatedState.restoreNodeResult?.isSuccess).isTrue()
        }
    }

    @Test
    fun `test that restoreNodeResult updated when calling restoreNodes failed`() = runTest {
        testScheduler.advanceUntilIdle()
        val node = mock<MegaNode> {
            on { handle }.thenReturn(1L)
            on { restoreHandle }.thenReturn(100L)
        }
        whenever(restoreNodesUseCase(mapOf(node.handle to node.restoreHandle)))
            .thenThrow(ForeignNodeException::class.java)

        underTest.state.test {
            val state = awaitItem()
            assertThat(state.restoreNodeResult).isNull()
            underTest.restoreNodes(mapOf(node.handle to node.restoreHandle))
            val updatedState = awaitItem()
            assertThat(updatedState.restoreNodeResult).isNotNull()
            assertThat(updatedState.restoreNodeResult?.isFailure).isTrue()
        }
    }

    @Test
    fun `test that nodeNameCollisionResult updated when calling checkRestoreNodesNameCollision successfully`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val node = mock<MegaNode> {
                on { handle }.thenReturn(1L)
                on { restoreHandle }.thenReturn(100L)
            }
            val result = mock<NodeNameCollisionsResult>()
            whenever(
                checkNodesNameCollisionUseCase(
                    mapOf(node.handle to node.restoreHandle),
                    NodeNameCollisionType.RESTORE
                )
            ).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.restoreNodeResult).isNull()
                underTest.checkRestoreNodesNameCollision(listOf(node))
                val updatedState = awaitItem()
                assertThat(updatedState.nodeNameCollisionsResult).isNotNull()
            }
        }

    @Test
    fun `test that moveRequestResult updated when calling moveNodesToRubbishBin successfully`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val result = mock<MoveRequestResult.RubbishMovement>()
            whenever(
                moveNodesToRubbishUseCase(any())
            ).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.moveNodesToRubbishBin(listOf(1L))
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isEqualTo(Result.success(result))
            }
        }

    @Test
    fun `test that moveRequestResult updated when calling markHandleMoveRequestResult`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val result = mock<MoveRequestResult.RubbishMovement>()
            whenever(
                moveNodesToRubbishUseCase(any())
            ).thenReturn(result)

            underTest.state.test {
                assertThat(awaitItem().moveRequestResult).isNull()
                underTest.moveNodesToRubbishBin(listOf(1L))
                assertThat(awaitItem().moveRequestResult).isEqualTo(Result.success(result))
                underTest.markHandleMoveRequestResult()
                assertThat(awaitItem().moveRequestResult).isNull()
            }
        }

    @Test
    fun `test that moveRequestResult updated when calling deleteNodesUseCase successfully`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val result = mock<MoveRequestResult.DeleteMovement>()
            whenever(
                deleteNodesUseCase(any())
            ).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.deleteNodes(listOf(1L))
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isEqualTo(Result.success(result))
            }
        }

    @Test
    fun `test that nodeNameCollisionResult updated when calling checkNodesNameCollisionUseCase successfully`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val nodes = listOf(1L, 2L)
            val targetNode = 100L
            val result = mock<NodeNameCollisionsResult>()
            whenever(
                checkNodesNameCollisionUseCase(
                    mapOf(1L to 100L, 2L to 100L),
                    NodeNameCollisionType.MOVE
                )
            ).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.nodeNameCollisionsResult).isNull()
                underTest.checkNodesNameCollision(nodes, targetNode, NodeNameCollisionType.MOVE)
                val updatedState = awaitItem()
                assertThat(updatedState.nodeNameCollisionsResult).isNotNull()
            }
        }

    @Test
    fun `test that moveRequestResult updated correctly when calling moveNodes successfully`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val result = mock<MoveRequestResult.GeneralMovement>()
            val nodes = mapOf(1L to 100L, 2L to 100L)
            whenever(moveNodesUseCase(nodes)).thenReturn(result)
            whenever(setMoveLatestTargetPathUseCase(100L)).thenReturn(Unit)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.moveNodes(nodes)
                val updatedState = awaitItem()
                verify(setMoveLatestTargetPathUseCase).invoke(100L)
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
            }
        }

    @Test
    fun `test that moveRequestResult updated correctly when calling moveNodes failed`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val nodes = mapOf(1L to 100L, 2L to 100L)
            whenever(moveNodesUseCase(nodes)).thenThrow(RuntimeException::class.java)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.moveNodes(nodes)
                verifyNoInteractions(setMoveLatestTargetPathUseCase)
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isFailure).isTrue()
            }
        }


    @Test
    fun `test that moveRequestResult updated correctly when calling copyNodes successfully`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val result = mock<MoveRequestResult.GeneralMovement>()
            val nodes = mapOf(1L to 100L, 2L to 100L)
            whenever(moveNodesUseCase(nodes)).thenReturn(result)
            whenever(setCopyLatestTargetPathUseCase(100L)).thenReturn(Unit)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.copyNodes(nodes)
                val updatedState = awaitItem()
                verify(setCopyLatestTargetPathUseCase).invoke(100L)
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isSuccess).isTrue()
            }
        }

    @Test
    fun `test that moveRequestResult updated correctly when calling copyNodes failed`() =
        runTest {
            testScheduler.advanceUntilIdle()
            val nodes = mapOf(1L to 100L, 2L to 100L)
            whenever(copyNodesUseCase(nodes)).thenThrow(RuntimeException::class.java)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.moveRequestResult).isNull()
                underTest.copyNodes(nodes)
                verifyNoInteractions(setCopyLatestTargetPathUseCase)
                val updatedState = awaitItem()
                assertThat(updatedState.moveRequestResult).isNotNull()
                assertThat(updatedState.moveRequestResult?.isFailure).isTrue()
            }
        }

    @Test
    fun `test that message update correctly when call removeShareUseCase success`() = runTest {
        val message = "message"
        testScheduler.advanceUntilIdle()
        whenever(removeShareResultMapper(any())).thenReturn(message)
        whenever(removeShareUseCase(any())).thenReturn(mock())
        underTest.state.test {
            assertThat(awaitItem().message).isNull()
            underTest.removeShares(listOf(1L))
            assertThat(awaitItem().message).isEqualTo(InfoToShow.RawString(message))
        }
    }

    @Test
    fun `test that message update correctly when call markHandledMessage`() = runTest {
        testScheduler.advanceUntilIdle()
        val message = "message"
        whenever(removeShareResultMapper(any())).thenReturn(message)
        whenever(removeShareUseCase(any())).thenReturn(mock())
        underTest.state.test {
            assertThat(awaitItem().message).isNull()
            underTest.removeShares(listOf(1L))
            assertThat(awaitItem().message).isEqualTo(InfoToShow.RawString(message))
        }
        underTest.markHandledMessage()
        underTest.state.test {
            assertThat(awaitItem().message).isNull()
        }
    }

    @Test
    fun `test that message update correctly when call disableExport success`() = runTest {
        val message = "message"
        testScheduler.advanceUntilIdle()
        whenever(removePublicLinkResultMapper(any())).thenReturn(message)
        whenever(disableExportNodesUseCase(any())).thenReturn(mock())
        underTest.state.test {
            assertThat(awaitItem().message).isNull()
            underTest.disableExport(listOf(1L))
            assertThat(awaitItem().message).isEqualTo(InfoToShow.RawString(message))
        }
    }

    @Test
    internal fun `test that dismiss calls the use case`() = runTest {
        val expected = 123
        underTest.dismissPsa(expected)
        testScheduler.advanceUntilIdle()
        verify(dismissPsaUseCase).invoke(expected)
    }

    @Test
    internal fun `test that getParentHandleForSearch returns parent handle based on search type`() =
        runTest {
            val expectedParentHandle = 678901
            val actual = underTest.getParentHandleForSearch(
                browserParentHandle = 123456,
                rubbishBinParentHandle = 234567,
                backupsParentHandle = 345678,
                incomingParentHandle = 456789,
                outgoingParentHandle = 567890,
                linksParentHandle = 678901,
                nodeSourceType = NodeSourceType.LINKS
            )
            assertThat(actual).isEqualTo(expectedParentHandle)
        }

    @Test
    fun `test that chatLinkContent updates correctly when call get chat link content returns`() =
        runTest {
            val link = "link"
            val linkContent = mock<ChatLinkContent.ChatLink>()
            testScheduler.advanceUntilIdle()
            whenever(getChatLinkContentUseCase(link)).thenReturn(linkContent)
            underTest.state.test {
                assertThat(awaitItem().chatLinkContent).isNull()
                underTest.checkLink("link")
                assertThat(awaitItem().chatLinkContent).isEqualTo(Result.success(linkContent))
            }
        }

    @Test
    fun `test that chatLinkContent resets to null when call markHandleCheckLinkResult`() =
        runTest {
            val link = "link"
            val linkContent = mock<ChatLinkContent.ChatLink>()
            testScheduler.advanceUntilIdle()
            whenever(getChatLinkContentUseCase(link)).thenReturn(linkContent)
            underTest.state.test {
                assertThat(awaitItem().chatLinkContent).isNull()
                underTest.checkLink("link")
                assertThat(awaitItem().chatLinkContent).isEqualTo(Result.success(linkContent))
                underTest.markHandleCheckLinkResult()
                assertThat(awaitItem().chatLinkContent).isNull()
            }
        }

    @Test
    fun `test that if Android Sync feature is on and syncs are not empty Sync service is enabled`() =
        runTest {
            val mockFolderPair = FolderPair(
                1L,
                "folder name",
                "folder name",
                RemoteFolder(1L, "folder name"),
                SyncStatus.SYNCING
            )
            testScheduler.advanceUntilIdle()
            monitorSyncsUseCaseFakeFlow.emit(listOf(mockFolderPair))
            testScheduler.advanceUntilIdle()
            underTest
                .state
                .test {
                    assertThat(awaitItem().androidSyncServiceEnabled).isTrue()
                }
        }

    @Test
    fun `test that if Android Sync feature is on and syncs are empty Sync service is disabled`() =
        runTest {
            monitorSyncsUseCaseFakeFlow.emit(listOf())
            testScheduler.advanceUntilIdle()

            underTest
                .state
                .test {
                    assertThat(awaitItem().androidSyncServiceEnabled).isFalse()
                }
        }

    @Test
    fun `test that the device center previous bottom navigation item is set`() = runTest {
        val previousItem = 3

        underTest.setDeviceCenterPreviousBottomNavigationItem(previousItem)
        underTest.state.test {
            assertThat(awaitItem().deviceCenterPreviousBottomNavigationItem).isEqualTo(previousItem)
        }
    }

    @ParameterizedTest(name = " when call status is {0}")
    @MethodSource("provideChatCallStatusParameters")
    fun `test that call is ended when user limit is reached`(chatCallStatus: ChatCallStatus) =
        runTest {
            whenever(
                getFeatureFlagValueUseCase(
                    ApiFeatures.CallUnlimitedProPlan
                )
            ).thenReturn(true)
            initViewModel()
            advanceUntilIdle()
            val call = mock<ChatCall> {
                on { status } doReturn chatCallStatus
                on { termCode } doReturn ChatCallTermCodeType.CallUsersLimit
            }
            fakeCallUpdatesFlow.emit(call)
            underTest.state.test {
                assertThat(awaitItem().callEndedDueToFreePlanLimits).isTrue()
            }
        }

    @ParameterizedTest(name = " when call status is {0}")
    @MethodSource("provideChatCallStatusParameters")
    fun `test that call is ended when duration limit is reached`(chatCallStatus: ChatCallStatus) =
        runTest {
            whenever(
                getFeatureFlagValueUseCase(
                    ApiFeatures.CallUnlimitedProPlan
                )
            ).thenReturn(true)
            initViewModel()
            advanceUntilIdle()
            val call = mock<ChatCall> {
                on { status } doReturn chatCallStatus
                on { termCode } doReturn ChatCallTermCodeType.CallDurationLimit
                on { isOwnClientCaller } doReturn true
            }
            fakeCallUpdatesFlow.emit(call)
            underTest.state.test {
                assertThat(awaitItem().shouldUpgradeToProPlan).isTrue()
            }
        }

    @Test
    fun `test that manager state is updated when search query is updated`() = runTest {
        val query = "query"
        underTest.updateSearchQuery(query)
        underTest.state.test {
            assertThat(awaitItem().searchQuery).isEqualTo(query)
        }
    }

    private fun provideChatCallStatusParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(ChatCallStatus.TerminatingUserParticipation),
        Arguments.of(ChatCallStatus.GenericNotification),
    )

    @Test
    fun `test that camera uploads automatically starts when the device begins charging`() =
        runTest {
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Connected)
            testScheduler.advanceUntilIdle()

            verify(startCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that camera uploads does not automatically start when the device is not charging`() =
        runTest {
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Disconnected)
            testScheduler.advanceUntilIdle()

            verifyNoInteractions(startCameraUploadUseCase)
        }

    @Test
    fun `test that camera uploads does not automatically start when the device charging state is unknown`() =
        runTest {
            monitorDevicePowerConnectionFakeFlow.emit(DevicePowerConnectionState.Unknown)
            testScheduler.advanceUntilIdle()

            verifyNoInteractions(startCameraUploadUseCase)
        }

    @Test
    fun `test that message update correctly when chat call update status emit the call with too many participants code`() =
        runTest {
            val call = mock<ChatCall> {
                on { status } doReturn ChatCallStatus.TerminatingUserParticipation
                on { termCode } doReturn ChatCallTermCodeType.TooManyParticipants
            }
            fakeCallUpdatesFlow.emit(call)
            testScheduler.advanceUntilIdle()
            underTest.state.test {
                assertThat(awaitItem().message).isEqualTo(InfoToShow.SimpleString(R.string.call_error_too_many_participants))
            }
        }


    @Test
    fun `test that state is updated correctly if upload a File`() = runTest {
        val file = File("path")
        val parentHandle = 123L
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                mapOf(file.absolutePath to null),
                NodeId(parentHandle)
            )
        )

        underTest.uploadFile(file, parentHandle)
        underTest.state.map { it.uploadEvent }.test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that state is updated correctly if upload a ShareInfo`() = runTest {
        val file = File("path")
        val path = file.absolutePath
        val parentHandle = 123L
        val pathsAndNames = mapOf(path to path)
        val expected = triggered(
            TransferTriggerEvent.StartUpload.Files(
                pathsAndNames,
                NodeId(parentHandle)
            )
        )

        underTest.uploadFiles(pathsAndNames, NodeId(parentHandle))
        underTest.state.map { it.uploadEvent }.test {
            assertThat(awaitItem()).isEqualTo(expected)
        }
    }

    @Test
    fun `test that prepareFiles invokes correctly`() = runTest {
        val uri = mock<Uri> {
            on { toString() } doReturn "uri"
        }

        underTest.prepareFiles(listOf(uri))
        verify(filePrepareUseCase).invoke(listOf(UriPath("uri")))
    }

    @Test
    fun `test that the old document scanner is used for scanning documents`() = runTest {
        val handleScanDocumentResult = HandleScanDocumentResult.UseLegacyImplementation
        whenever(scannerHandler.handleScanDocument()).thenReturn(handleScanDocumentResult)

        underTest.handleScanDocument()
        testScheduler.advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().handleScanDocumentResult).isEqualTo(handleScanDocumentResult)
        }
    }

    @Test
    fun `test that the new ML Document Kit Scanner is used for scanning documents`() = runTest {
        val handleScanDocumentResult = HandleScanDocumentResult.UseNewImplementation(mock())
        whenever(scannerHandler.handleScanDocument()).thenReturn(handleScanDocumentResult)

        underTest.handleScanDocument()
        testScheduler.advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().handleScanDocumentResult).isEqualTo(handleScanDocumentResult)
        }
    }

    @Test
    fun `test that the handle scan document result is reset`() = runTest {
        underTest.onHandleScanDocumentResultConsumed()
        testScheduler.advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().handleScanDocumentResult).isNull()
        }
    }

    @Test
    fun `test that the document scanning error type is reset`() = runTest {
        underTest.onDocumentScanningErrorConsumed()
        testScheduler.advanceUntilIdle()

        underTest.state.test {
            assertThat(awaitItem().documentScanningErrorTypeUiItem).isNull()
        }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
