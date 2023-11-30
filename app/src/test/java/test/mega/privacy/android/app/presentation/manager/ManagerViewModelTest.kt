package test.mega.privacy.android.app.presentation.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetBackupsNode
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkResultMapper
import mega.privacy.android.app.main.dialog.shares.RemoveShareResultMapper
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.CameraUploadsFolderDestinationUpdate
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.MyAccountUpdate.Action
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.entity.camerauploads.CameraUploadFolderType
import mega.privacy.android.domain.entity.chat.ChatLinkContent
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.entity.user.UserUpdate
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlertsUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.HasBackupsChildren
import mega.privacy.android.domain.usecase.MonitorOfflineFileAvailabilityUseCase
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.account.GetFullAccountInfoUseCase
import mega.privacy.android.domain.usecase.account.GetIncomingContactRequestsUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.account.RenameRecoveryKeyFileUseCase
import mega.privacy.android.domain.usecase.account.RequireTwoFactorAuthenticationUseCase
import mega.privacy.android.domain.usecase.account.SetCopyLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.account.SetMoveLatestTargetPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.AreCameraUploadsFoldersInRubbishBinUseCase
import mega.privacy.android.domain.usecase.camerauploads.EstablishCameraUploadsSyncHandlesUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsFolderDestinationUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.chat.GetNumUnreadChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.contact.SaveContactByEmailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallRecordingConsentAcceptedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
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
import mega.privacy.android.domain.usecase.photos.mediadiscovery.SendStatisticsMediaDiscoveryUseCase
import mega.privacy.android.domain.usecase.psa.DismissPsaUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.domain.usecase.transfers.completed.DeleteOldestCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.workers.StartCameraUploadUseCase
import mega.privacy.android.domain.usecase.workers.StopCameraUploadsUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.domain.usecase.FakeMonitorBackupFolder
import kotlin.test.assertFalse

@ExperimentalCoroutinesApi
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val monitorGlobalUpdates = MutableStateFlow<GlobalUpdate>(GlobalUpdate.OnReloadNeeded)
    private val monitorNodeUpdates = MutableSharedFlow<NodeUpdate>()
    private val monitorContactUpdates = MutableSharedFlow<UserUpdate>()
    private val monitorSecurityUpgradeInApp = MutableStateFlow(false)
    private val getNumUnreadUserAlertsUseCase =
        mock<GetNumUnreadUserAlertsUseCase> { onBlocking { invoke() }.thenReturn(0) }
    private val hasBackupsChildren =
        mock<HasBackupsChildren> { onBlocking { invoke() }.thenReturn(false) }
    private val monitorContactRequestUpdates = MutableStateFlow(emptyList<ContactRequest>())

    private val initialIsFirsLoginValue = true
    private val sendStatisticsMediaDiscoveryUseCase = mock<SendStatisticsMediaDiscoveryUseCase>()
    private val savedStateHandle = SavedStateHandle(
        mapOf(
            ManagerViewModel.isFirstLoginKey to initialIsFirsLoginValue
        )
    )
    private val getBackupsNode = mock<GetBackupsNode> { onBlocking { invoke() }.thenReturn(null) }
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
    private val areCameraUploadsFoldersInRubbishBinUseCase =
        mock<AreCameraUploadsFoldersInRubbishBinUseCase>()
    private val getCloudSortOrder =
        mock<GetCloudSortOrder> { onBlocking { invoke() }.thenReturn(SortOrder.ORDER_ALPHABETICAL_ASC) }
    private val isConnectedToInternetUseCase = mock<IsConnectedToInternetUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorSyncStalledIssuesUseCase = mock<MonitorSyncStalledIssuesUseCase>()
    private val getFeatureFlagValueUseCase =
        mock<GetFeatureFlagValueUseCase> { onBlocking { invoke(any()) }.thenReturn(false) }
    private val shareDataList = listOf(
        ShareData(
            user = "user",
            nodeHandle = 8766L,
            access = AccessPermission.READ,
            timeStamp = 987654678L,
            isPending = true,
            isVerified = false,
        ),
        ShareData(
            user = "user",
            nodeHandle = 8766L,
            access = AccessPermission.READ,
            timeStamp = 987654678L,
            isPending = true,
            isVerified = false,
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
    private val monitorUserUpdates = mock<MonitorUserUpdates>()
    private val monitorMyAccountUpdateUseCase = mock<MonitorMyAccountUpdateUseCase> {
        onBlocking { invoke() }.thenReturn(
            flowOf(
                MyAccountUpdate(Action.STORAGE_STATE_CHANGED, StorageState.Green),
                MyAccountUpdate(Action.UPDATE_ACCOUNT_DETAILS, null)
            )
        )
    }
    private val establishCameraUploadsSyncHandlesUseCase =
        mock<EstablishCameraUploadsSyncHandlesUseCase>()
    private val startCameraUploadUseCase = mock<StartCameraUploadUseCase>()
    private val stopCameraUploadsUseCase = mock<StopCameraUploadsUseCase>()
    private val saveContactByEmailUseCase = mock<SaveContactByEmailUseCase>()
    private val createShareKey = mock<CreateShareKey>()
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
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat = mock()
    private val getChatCallUseCase: GetChatCallUseCase = mock()
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase = mock()
    private val answerChatCallUseCase: AnswerChatCallUseCase = mock()
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase = mock()
    private val rtcAudioManagerGateway: RTCAudioManagerGateway = mock()
    private val chatManagement: ChatManagement = mock()
    private val passcodeManagement: PasscodeManagement = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase = mock()
    private val hangChatCallUseCase: HangChatCallUseCase = mock()
    private val monitorCallRecordingConsentAcceptedUseCase: MonitorCallRecordingConsentAcceptedUseCase =
        mock()
    private val getNodeByHandle: GetNodeByHandle = mock()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = ManagerViewModel(
            monitorNodeUpdates = { monitorNodeUpdates },
            monitorContactUpdates = { monitorContactUpdates },
            monitorGlobalUpdates = { monitorGlobalUpdates },
            monitorContactRequestUpdates = mock {
                onBlocking { invoke() }.thenReturn(monitorContactRequestUpdates)
            },
            getNumUnreadUserAlertsUseCase = getNumUnreadUserAlertsUseCase,
            hasBackupsChildren = hasBackupsChildren,
            sendStatisticsMediaDiscoveryUseCase = sendStatisticsMediaDiscoveryUseCase,
            savedStateHandle = savedStateHandle,
            getBackupsNode = getBackupsNode,
            monitorStorageStateEventUseCase = monitorStorageState,
            monitorCameraUploadsFolderDestinationUseCase = monitorCameraUploadsFolderDestinationUpdateUseCase,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            areCameraUploadsFoldersInRubbishBinUseCase = areCameraUploadsFoldersInRubbishBinUseCase,
            getCloudSortOrder = getCloudSortOrder,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            isConnectedToInternetUseCase = isConnectedToInternetUseCase,
            getExtendedAccountDetail = mock(),
            getFullAccountInfoUseCase = getFullAccountInfoUseCase,
            getActiveSubscriptionUseCase = mock(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getUnverifiedIncomingShares = getUnverifiedIncomingShares,
            getUnverifiedOutgoingShares = getUnverifiedOutgoingShares,
            monitorFinishActivityUseCase = monitorFinishActivity,
            requireTwoFactorAuthenticationUseCase = requireTwoFactorAuthenticationUseCase,
            setCopyLatestTargetPathUseCase = setCopyLatestTargetPathUseCase,
            setMoveLatestTargetPathUseCase = setMoveLatestTargetPathUseCase,
            monitorSecurityUpgradeInApp = { monitorSecurityUpgradeInApp },
            listenToNewMediaUseCase = mock(),
            monitorUserUpdates = monitorUserUpdates,
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase,
            establishCameraUploadsSyncHandlesUseCase = establishCameraUploadsSyncHandlesUseCase,
            monitorUpdatePushNotificationSettingsUseCase = monitorPushNotificationSettingsUpdate,
            startCameraUploadUseCase = startCameraUploadUseCase,
            stopCameraUploadsUseCase = stopCameraUploadsUseCase,
            createShareKey = createShareKey,
            saveContactByEmailUseCase = saveContactByEmailUseCase,
            deleteOldestCompletedTransfersUseCase = deleteOldestCompletedTransfersUseCase,
            monitorOfflineNodeAvailabilityUseCase = monitorOfflineNodeAvailabilityUseCase,
            getIncomingContactRequestsUseCase = getIncomingContactRequestUseCase,
            monitorChatArchivedUseCase = monitorChatArchivedUseCase,
            restoreNodesUseCase = restoreNodesUseCase,
            checkNodesNameCollisionUseCase = checkNodesNameCollisionUseCase,
            monitorBackupFolder = monitorBackupFolder,
            moveNodesToRubbishUseCase = moveNodesToRubbishUseCase,
            deleteNodesUseCase = deleteNodesUseCase,
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
            getScheduledMeetingByChat = getScheduledMeetingByChat,
            getChatCallUseCase = getChatCallUseCase,
            startMeetingInWaitingRoomChatUseCase = startMeetingInWaitingRoomChatUseCase,
            answerChatCallUseCase = answerChatCallUseCase,
            setChatVideoInDeviceUseCase = setChatVideoInDeviceUseCase,
            rtcAudioManagerGateway = rtcAudioManagerGateway,
            chatManagement = chatManagement,
            passcodeManagement = passcodeManagement,
            monitorSyncStalledIssuesUseCase = monitorSyncStalledIssuesUseCase,
            monitorSyncsUseCase = monitorSyncsUseCase,
            monitorChatSessionUpdatesUseCase = monitorChatSessionUpdatesUseCase,
            hangChatCallUseCase = hangChatCallUseCase,
            monitorCallRecordingConsentAcceptedUseCase = monitorCallRecordingConsentAcceptedUseCase,
            getNodeByHandle = getNodeByHandle,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
            monitorNodeUpdates.emit(NodeUpdate(emptyMap()))
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that updateToolbarTitle is set when calling setUpdateToolbar`() = runTest {
        testScheduler.advanceUntilIdle()
        underTest.state.map { it.nodeUpdateReceived }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            monitorNodeUpdates.emit(NodeUpdate(emptyMap()))
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
            underTest.monitorMyAccountUpdateEvent.distinctUntilChanged().test {
                assertThat(awaitItem().action).isEqualTo(Action.STORAGE_STATE_CHANGED)
                monitorFinishActivity()
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

            whenever(monitorUserUpdates()).thenReturn(userUpdates.asFlow())
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
    fun `test that when startCameraUpload is called, startCameraUploadUseCase is called`() =
        runTest {
            underTest.startCameraUpload()
            testScheduler.advanceUntilIdle()
            verify(startCameraUploadUseCase).invoke()
        }

    @Test
    fun `test that when stopCameraUpload is called, stopCameraUploadUseCase is called`() =
        runTest {
            val shouldReschedule = true
            underTest.stopCameraUploads(shouldReschedule)
            testScheduler.advanceUntilIdle()
            verify(stopCameraUploadsUseCase).invoke(shouldReschedule = shouldReschedule)
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
            monitorContactRequestUpdates.emit(
                contactRequests
            )
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
            monitorGlobalUpdates.emit(GlobalUpdate.OnUserAlertsUpdate(arrayListOf()))
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
            monitorContactRequestUpdates.emit(contactRequests)
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
            monitorContactRequestUpdates.emit(contactRequests)
            advanceUntilIdle()
            verify(saveContactByEmailUseCase).invoke("targetEmail@mega.co.nz")
        }

    @Test
    fun `test that numUnreadUserAlerts update when there is accepted contact request event`() =
        runTest {
            advanceUntilIdle()
            assertThat(underTest.onGetNumUnreadUserAlerts().test().value().second).isEqualTo(0)
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
            whenever(getNumUnreadUserAlertsUseCase()).thenReturn(3)
            monitorContactRequestUpdates.emit(contactRequests)
            advanceUntilIdle()
            assertThat(underTest.onGetNumUnreadUserAlerts().test().value().second).isEqualTo(3)
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
            val result = mock<NodeNameCollisionResult>()
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
                assertThat(updatedState.nodeNameCollisionResult).isNotNull()
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
            val result = mock<NodeNameCollisionResult>()
            whenever(
                checkNodesNameCollisionUseCase(
                    mapOf(1L to 100L, 2L to 100L),
                    NodeNameCollisionType.MOVE
                )
            ).thenReturn(result)

            underTest.state.test {
                val state = awaitItem()
                assertThat(state.nodeNameCollisionResult).isNull()
                underTest.checkNodesNameCollision(nodes, targetNode, NodeNameCollisionType.MOVE)
                val updatedState = awaitItem()
                assertThat(updatedState.nodeNameCollisionResult).isNotNull()
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
    fun `test that camera uploads is stopped if a camera uploads primary folder update is received and primary folder is in rubbish bin`() =
        runTest {
            testScheduler.advanceUntilIdle()

            val primaryHandle = 11111L
            val secondaryHandle = 22222L
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { id }.thenReturn(NodeId(primaryHandle)) },
                        listOf()
                    )
                )
            )

            whenever(getPrimarySyncHandleUseCase()).thenReturn(primaryHandle)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(secondaryHandle)
            whenever(areCameraUploadsFoldersInRubbishBinUseCase(primaryHandle, secondaryHandle))
                .thenReturn(true)
            monitorNodeUpdates.emit(nodeUpdate)
            testScheduler.advanceUntilIdle()
            verify(stopCameraUploadsUseCase).invoke(shouldReschedule = true)
        }

    @Test
    fun `test that camera uploads is stopped if a camera uploads secondary folder update is received and secondary folder is enabled in rubbish bin`() =
        runTest {
            testScheduler.advanceUntilIdle()

            val primaryHandle = 11111L
            val secondaryHandle = 22222L
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { id }.thenReturn(NodeId(secondaryHandle)) },
                        listOf()
                    )
                )
            )

            whenever(getPrimarySyncHandleUseCase()).thenReturn(primaryHandle)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(secondaryHandle)
            whenever(areCameraUploadsFoldersInRubbishBinUseCase(primaryHandle, secondaryHandle))
                .thenReturn(true)
            monitorNodeUpdates.emit(nodeUpdate)
            testScheduler.advanceUntilIdle()
            verify(stopCameraUploadsUseCase).invoke(shouldReschedule = true)
        }

    @Test
    fun `test that camera uploads is not stopped if a node update is received and is not the camera uploads primary or secondary folder`() =
        runTest {
            testScheduler.advanceUntilIdle()

            val nodeHandle = 88888L
            val primaryHandle = 11111L
            val secondaryHandle = 22222L
            val nodeUpdate = NodeUpdate(
                mapOf(
                    Pair(
                        mock { on { primaryHandle }.thenReturn(nodeHandle) },
                        listOf()
                    )
                )
            )

            whenever(getPrimarySyncHandleUseCase()).thenReturn(primaryHandle)
            whenever(getSecondarySyncHandleUseCase()).thenReturn(secondaryHandle)
            whenever(areCameraUploadsFoldersInRubbishBinUseCase(primaryHandle, secondaryHandle))
                .thenReturn(true)
            monitorNodeUpdates.emit(nodeUpdate)
            testScheduler.advanceUntilIdle()
            verifyNoInteractions(stopCameraUploadsUseCase)
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
            assertThat(awaitItem().message).isEqualTo(message)
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
            assertThat(awaitItem().message).isEqualTo(message)
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
            assertThat(awaitItem().message).isEqualTo(message)
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
                searchType = SearchType.LINKS
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.AndroidSync)).thenReturn(true)
            whenever(monitorSyncsUseCase()).thenReturn(flowOf(listOf(mockFolderPair)))
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
            whenever(getFeatureFlagValueUseCase(AppFeatures.AndroidSync)).thenReturn(true)
            whenever(monitorSyncsUseCase()).thenReturn(flowOf((listOf())))
            testScheduler.advanceUntilIdle()

            underTest
                .state
                .test {
                    assertThat(awaitItem().androidSyncServiceEnabled).isFalse()
                }
        }

    @Test
    fun `test that if Android Sync feature is off Sync service is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(AppFeatures.AndroidSync)).thenReturn(false)
            testScheduler.advanceUntilIdle()

            underTest
                .state
                .test {
                    assertThat(awaitItem().androidSyncServiceEnabled).isFalse()
                }
        }

    @Test
    fun `test that a specific mega node is retrieved by handle`() = runTest {
        val megaNodeHandle = 123456L
        val megaNode = mock<MegaNode> {
            on { handle }.thenReturn(megaNodeHandle)
        }
        whenever(getNodeByHandle(any())).thenReturn(megaNode)

        val result = underTest.retrieveMegaNode(123456L)
        assertThat(result).isEqualTo(megaNode)
    }
}
