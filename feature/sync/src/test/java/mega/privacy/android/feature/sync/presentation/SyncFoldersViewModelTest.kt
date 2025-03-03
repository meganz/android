package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.BatteryInfo
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.GetNodePathByIdUseCase
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadsBackupUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetMediaUploadsBackupUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsSettingsActionsUseCase
import mega.privacy.android.domain.usecase.camerauploads.MonitorCameraUploadsStatusInfoUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.domain.usecase.node.MoveDeconfiguredBackupNodesUseCase
import mega.privacy.android.domain.usecase.node.RemoveDeconfiguredBackupNodesUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RefreshSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.option.SetUserPausedSyncUseCase
import mega.privacy.android.feature.sync.ui.mapper.sync.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.StopBackupOption
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersUiState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncFoldersViewModelTest {

    private val syncUiItemMapper: SyncUiItemMapper = mock()
    private val removeFolderPairUseCase: RemoveFolderPairUseCase = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val resumeSyncUseCase: ResumeSyncUseCase = mock()
    private val pauseSyncUseCase: PauseSyncUseCase = mock()
    private val monitorStalledIssuesUseCase: MonitorSyncStalledIssuesUseCase = mock()
    private val setUserPausedSyncsUseCase: SetUserPausedSyncUseCase = mock()
    private val refreshSyncUseCase: RefreshSyncUseCase = mock()
    private val monitorBatteryInfoUseCase: MonitorBatteryInfoUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()
    private val getNodePathByIdUseCase: GetNodePathByIdUseCase = mock()
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val moveDeconfiguredBackupNodesUseCase: MoveDeconfiguredBackupNodesUseCase = mock()
    private val removeDeconfiguredBackupNodesUseCase: RemoveDeconfiguredBackupNodesUseCase = mock()
    private val getCameraUploadsBackupUseCase: GetCameraUploadsBackupUseCase = mock()
    private val getMediaUploadsBackupUseCase: GetMediaUploadsBackupUseCase = mock()
    private val monitorCameraUploadsSettingsActionsUseCase: MonitorCameraUploadsSettingsActionsUseCase =
        mock()
    private val monitorCameraUploadsStatusInfoUseCase: MonitorCameraUploadsStatusInfoUseCase =
        mock()
    private lateinit var underTest: SyncFoldersViewModel

    private val folderPairs = listOf(
        FolderPair(
            id = 3L,
            syncType = SyncType.TYPE_TWOWAY,
            pairName = "folderPair",
            localFolderPath = "DCIM",
            remoteFolder = RemoteFolder(id = NodeId(233L), name = "photos"),
            syncStatus = SyncStatus.SYNCING
        )
    )

    private val syncUiItems = listOf(
        SyncUiItem(
            id = 3L,
            syncType = SyncType.TYPE_TWOWAY,
            folderPairName = "folderPair",
            status = SyncStatus.SYNCING,
            deviceStoragePath = "DCIM",
            hasStalledIssues = true,
            megaStoragePath = "photos",
            megaStorageNodeId = NodeId(1234L),
            expanded = false
        ),
        SyncUiItem(
            id = 4L,
            syncType = SyncType.TYPE_BACKUP,
            folderPairName = "folderPair",
            status = SyncStatus.SYNCING,
            deviceStoragePath = "Backup",
            hasStalledIssues = false,
            megaStoragePath = "Backup",
            megaStorageNodeId = NodeId(5678L),
            expanded = false
        ),
    )

    private val stalledIssues = listOf(
        StalledIssue(
            syncId = 1L,
            nodeIds = listOf(NodeId(3L)),
            localPaths = listOf("DCIM/photo.jpg"),
            issueType = StallIssueType.DownloadIssue,
            conflictName = "conflicting folder",
            nodeNames = listOf("Camera"),
        )
    )

    @BeforeEach
    fun setupMock(): Unit = runBlocking {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        whenever(monitorBatteryInfoUseCase()).thenReturn(flowOf(BatteryInfo(100, true)))

        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_I
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }
        whenever(monitorAccountDetailUseCase()).thenReturn(
            flowOf(accountDetail)
        )

        whenever(monitorStalledIssuesUseCase()).thenReturn(flowOf(emptyList()))
    }

    @AfterEach
    fun resetAndTearDown() {
        reset(
            monitorSyncsUseCase,
            syncUiItemMapper,
            removeFolderPairUseCase,
            resumeSyncUseCase,
            pauseSyncUseCase,
            monitorStalledIssuesUseCase,
            setUserPausedSyncsUseCase,
            isStorageOverQuotaUseCase,
            monitorAccountDetailUseCase,
            getRootNodeUseCase,
            moveDeconfiguredBackupNodesUseCase,
            getNodePathByIdUseCase,
            getCameraUploadsBackupUseCase,
            monitorCameraUploadsStatusInfoUseCase,
        )
    }

    @Test
    fun `test that viewmodel fetches all folder pairs upon initialization`() = runTest {
        whenever(isStorageOverQuotaUseCase()).thenReturn(false)
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncFoldersUiState(syncUiItems)

        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that card click change the state to expanded`() = runTest {
        whenever(isStorageOverQuotaUseCase()).thenReturn(false)
        whenever(monitorStalledIssuesUseCase()).thenReturn(flowOf(emptyList()))
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncFoldersUiState(
            syncUiItems.map { if (it == syncUiItems.first()) it.copy(expanded = true) else it },
        )

        initViewModel()
        underTest.handleAction(
            SyncFoldersAction.CardExpanded(syncUiItems.first(), true)
        )

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that remove action changes the state to display the confirm dialog`() = runTest {
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val syncUiItem = syncUiItems.first()
        val expectedState =
            SyncFoldersUiState(
                syncUiItems = syncUiItems,
                showConfirmRemoveSyncFolderDialog = true,
                syncUiItemToRemove = syncUiItem,
            )
        initViewModel()
        underTest.handleAction(
            SyncFoldersAction.RemoveFolderClicked(syncUiItem = syncUiItem)
        )

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that confirm the remove action for a Sync removes folder pair`() = runTest {
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val syncUiItem = syncUiItems.first { it.syncType == SyncType.TYPE_TWOWAY }
        whenever(removeFolderPairUseCase(syncUiItem.id)).thenReturn(Unit)
        initViewModel()
        underTest.handleAction(SyncFoldersAction.RemoveFolderClicked(syncUiItem))
        underTest.handleAction(
            SyncFoldersAction.OnRemoveSyncFolderDialogConfirmed
        )

        verify(removeFolderPairUseCase).invoke(folderPairId = syncUiItem.id)
    }

    @Test
    fun `test that confirm the remove action for a Backup with move option removes folder pair and moves remote folder to Cloud Drive`() =
        runTest {
            val rootFolder: FolderNode = mock {
                on { id } doReturn NodeId(123456L)
            }
            whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
            val syncUiItem = syncUiItems.first { it.syncType == SyncType.TYPE_BACKUP }
            whenever(removeFolderPairUseCase(syncUiItem.id)).thenReturn(Unit)
            whenever(getRootNodeUseCase()).thenReturn(rootFolder)
            initViewModel()
            underTest.handleAction(SyncFoldersAction.RemoveFolderClicked(syncUiItem))
            underTest.handleAction(
                SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed(
                    stopBackupOption = StopBackupOption.MOVE,
                    selectedFolder = null,
                )
            )

            verify(removeFolderPairUseCase).invoke(folderPairId = syncUiItem.id)
            verify(moveDeconfiguredBackupNodesUseCase).invoke(
                deconfiguredBackupRoot = syncUiItem.megaStorageNodeId,
                backupDestination = rootFolder.id
            )
        }

    @Test
    fun `test that confirm the remove action for a Backup with move option removes folder pair and moves remote folder to selected folder`() =
        runTest {
            val remoteFolder = RemoteFolder(NodeId(123L), "Selected Folder")
            whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
            val syncUiItem = syncUiItems.first { it.syncType == SyncType.TYPE_BACKUP }
            whenever(removeFolderPairUseCase(syncUiItem.id)).thenReturn(Unit)
            initViewModel()
            underTest.handleAction(SyncFoldersAction.RemoveFolderClicked(syncUiItem))
            underTest.handleAction(
                SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed(
                    stopBackupOption = StopBackupOption.MOVE,
                    selectedFolder = remoteFolder,
                )
            )

            verify(removeFolderPairUseCase).invoke(folderPairId = syncUiItem.id)
            verify(moveDeconfiguredBackupNodesUseCase).invoke(
                deconfiguredBackupRoot = syncUiItem.megaStorageNodeId,
                backupDestination = remoteFolder.id
            )
        }

    @Test
    fun `test that confirm the remove action for a Backup with delete option removes folder pair and delete remote folder`() =
        runTest {
            whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
            val syncUiItem = syncUiItems.first { it.syncType == SyncType.TYPE_BACKUP }
            whenever(removeFolderPairUseCase(syncUiItem.id)).thenReturn(Unit)
            initViewModel()
            underTest.handleAction(SyncFoldersAction.RemoveFolderClicked(syncUiItem))
            underTest.handleAction(
                SyncFoldersAction.OnRemoveBackupFolderDialogConfirmed(
                    stopBackupOption = StopBackupOption.DELETE,
                    selectedFolder = null,
                )
            )

            verify(removeFolderPairUseCase).invoke(folderPairId = syncUiItem.id)
            verify(removeDeconfiguredBackupNodesUseCase).invoke(
                deconfiguredBackupRoot = syncUiItem.megaStorageNodeId,
            )
        }

    @Test
    fun `test that cancel the remove action resets the state to do not display the confirm dialog`() =
        runTest {
            whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
            val expectedState =
                SyncFoldersUiState(
                    syncUiItems = syncUiItems,
                    showConfirmRemoveSyncFolderDialog = false,
                    syncUiItemToRemove = null,
                )
            initViewModel()
            underTest.handleAction(
                SyncFoldersAction.OnRemoveFolderDialogDismissed
            )

            underTest.uiState.test {
                assertThat(awaitItem()).isEqualTo(expectedState)
            }
        }

    @Test
    fun `test that view model pause run click pauses sync if sync is not paused`() = runTest {
        val syncUiItem = getSyncUiItem(SyncStatus.SYNCING)
        initViewModel()

        underTest.handleAction(SyncFoldersAction.PauseRunClicked(syncUiItem))

        verify(pauseSyncUseCase).invoke(syncUiItem.id)
        verify(setUserPausedSyncsUseCase).invoke(syncUiItem.id, true)
    }

    @Test
    fun `test that view model pause run clicked runs sync if sync is paused`() = runTest {
        val syncUiItem = getSyncUiItem(SyncStatus.PAUSED)
        initViewModel()

        underTest.handleAction(SyncFoldersAction.PauseRunClicked(syncUiItem))

        verify(resumeSyncUseCase).invoke(syncUiItem.id)
        verify(setUserPausedSyncsUseCase).invoke(syncUiItem.id, false)
    }

    @Test
    fun `test that the folder is in error status when the stalled issues are not empty`() =
        runTest {
            whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
            whenever(monitorStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
            val expectedState = SyncFoldersUiState(
                syncUiItems.map { if (it == syncUiItems.first()) it.copy(hasStalledIssues = true) else it },
            )

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem()).isEqualTo(expectedState)
            }
        }

    @Test
    fun `test that storage over quota use case returns true changes ui state to show storage overquota`() =
        runTest {
            whenever(isStorageOverQuotaUseCase()).thenReturn(true)
            initViewModel()
            assertThat(underTest.uiState.value.isStorageOverQuota).isTrue()
        }

    @Test
    fun `test that storage over quota use case returns false changes ui state to not show storage overquota`() =
        runTest {
            whenever(isStorageOverQuotaUseCase()).thenReturn(false)
            initViewModel()
            assertThat(underTest.uiState.value.isStorageOverQuota).isFalse()
        }

    private fun getSyncUiItem(status: SyncStatus): SyncUiItem = SyncUiItem(
        id = 3L,
        syncType = SyncType.TYPE_TWOWAY,
        folderPairName = "folderPair",
        status = status,
        deviceStoragePath = "DCIM",
        megaStoragePath = "photos",
        megaStorageNodeId = NodeId(1234L),
        hasStalledIssues = false,
        expanded = false
    )

    private fun initViewModel() {
        underTest = SyncFoldersViewModel(
            syncUiItemMapper = syncUiItemMapper,
            removeFolderPairUseCase = removeFolderPairUseCase,
            monitorSyncsUseCase = monitorSyncsUseCase,
            resumeSyncUseCase = resumeSyncUseCase,
            pauseSyncUseCase = pauseSyncUseCase,
            monitorStalledIssuesUseCase = monitorStalledIssuesUseCase,
            setUserPausedSyncsUseCase = setUserPausedSyncsUseCase,
            refreshSyncUseCase = refreshSyncUseCase,
            monitorBatteryInfoUseCase = monitorBatteryInfoUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            getNodePathByIdUseCase = getNodePathByIdUseCase,
            getFolderTreeInfo = getFolderTreeInfo,
            isStorageOverQuotaUseCase = isStorageOverQuotaUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            moveDeconfiguredBackupNodesUseCase = moveDeconfiguredBackupNodesUseCase,
            removeDeconfiguredBackupNodesUseCase = removeDeconfiguredBackupNodesUseCase,
            getCameraUploadsBackupUseCase = getCameraUploadsBackupUseCase,
            getMediaUploadsBackupUseCase = getMediaUploadsBackupUseCase,
            monitorCameraUploadsSettingsActionsUseCase = monitorCameraUploadsSettingsActionsUseCase,
            monitorCameraUploadsStatusInfoUseCase = monitorCameraUploadsStatusInfoUseCase,
        )
    }
}
