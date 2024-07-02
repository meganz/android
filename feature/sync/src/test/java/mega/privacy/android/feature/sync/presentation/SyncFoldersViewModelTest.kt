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
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetFolderTreeInfo
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.IsProAccountUseCase
import mega.privacy.android.domain.usecase.account.IsStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.environment.MonitorBatteryInfoUseCase
import mega.privacy.android.feature.sync.R
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
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersState
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
    private val getFolderTreeInfo: GetFolderTreeInfo = mock()
    private val isStorageOverQuotaUseCase: IsStorageOverQuotaUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val isProAccountUseCase: IsProAccountUseCase = mock()
    private lateinit var underTest: SyncFoldersViewModel

    private val folderPairs = listOf(
        FolderPair(
            3L, "folderPair", "DCIM", RemoteFolder(233L, "photos"), SyncStatus.SYNCING
        )
    )

    private val syncUiItems = listOf(
        SyncUiItem(
            id = 3L,
            folderPairName = "folderPair",
            status = SyncStatus.SYNCING,
            deviceStoragePath = "DCIM",
            hasStalledIssues = false,
            megaStoragePath = "photos",
            megaStorageNodeId = NodeId(1234L),
            method = R.string.sync_two_way,
            expanded = false
        )
    )

    private val stalledIssues = listOf(
        StalledIssue(
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

        whenever(isProAccountUseCase()).thenReturn(true)
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
            isProAccountUseCase
        )
    }

    @Test
    fun `test that viewmodel fetches all folder pairs upon initialization`() = runTest {
        whenever(isStorageOverQuotaUseCase()).thenReturn(false)
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncFoldersState(syncUiItems, isFreeAccount = false)

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
        val expectedState =
            SyncFoldersState(syncUiItems.map { it.copy(expanded = true) }, isFreeAccount = false)

        initViewModel()
        underTest.handleAction(
            SyncFoldersAction.CardExpanded(syncUiItems.first(), true)
        )

        underTest.uiState.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that remove action removes folder pair`() = runTest {
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val folderPairId = 9999L
        whenever(removeFolderPairUseCase(folderPairId)).thenReturn(Unit)
        whenever(monitorStalledIssuesUseCase()).thenReturn(flowOf(stalledIssues))
        initViewModel()
        underTest.handleAction(
            SyncFoldersAction.RemoveFolderClicked(folderPairId)
        )

        verify(removeFolderPairUseCase).invoke(folderPairId)
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
            val expectedState =
                SyncFoldersState(
                    syncUiItems.map { it.copy(hasStalledIssues = true) },
                    isFreeAccount = false
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

    @Test
    fun `test that if user is a free user and has syncs need to show dialog`() = runTest {
        whenever(isStorageOverQuotaUseCase()).thenReturn(false)
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        whenever(isProAccountUseCase()).thenReturn(false)
        whenever(monitorAccountDetailUseCase()).thenReturn(flowOf())

        initViewModel()

        assertThat(underTest.uiState.value.showSyncsPausedErrorDialog).isTrue()
        assertThat(underTest.uiState.value.isFreeAccount).isTrue()
    }

    private fun getSyncUiItem(status: SyncStatus): SyncUiItem = SyncUiItem(
        id = 3L,
        folderPairName = "folderPair",
        status = status,
        deviceStoragePath = "DCIM",
        megaStoragePath = "photos",
        megaStorageNodeId = NodeId(1234L),
        hasStalledIssues = false,
        method = R.string.sync_two_way,
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
            getFolderTreeInfo = getFolderTreeInfo,
            isStorageOverQuotaUseCase = isStorageOverQuotaUseCase,
            isProAccountUseCase = isProAccountUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase
        )
    }
}
