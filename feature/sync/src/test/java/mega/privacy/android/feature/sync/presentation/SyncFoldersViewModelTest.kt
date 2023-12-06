package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.StallIssueType
import mega.privacy.android.feature.sync.domain.entity.StalledIssue
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.GetSyncStalledIssuesUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResumeSyncUseCase
import mega.privacy.android.feature.sync.ui.mapper.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersAction
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersState
import mega.privacy.android.feature.sync.ui.synclist.folders.SyncFoldersViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncFoldersViewModelTest {

    private val syncUiItemMapper: SyncUiItemMapper = mock()
    private val removeFolderPairUseCase: RemoveFolderPairUseCase = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val resumeSyncUseCase: ResumeSyncUseCase = mock()
    private val pauseSyncUseCase: PauseSyncUseCase = mock()
    private val getSyncStalledIssuesUseCase: GetSyncStalledIssuesUseCase = mock()

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
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
        reset(
            monitorSyncsUseCase,
            syncUiItemMapper,
            removeFolderPairUseCase,
            resumeSyncUseCase,
            pauseSyncUseCase
        )
    }

    @Test
    fun `test that viewmodel fetches all folder pairs upon initialization`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        whenever(getSyncStalledIssuesUseCase()).thenReturn(emptyList())
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncFoldersState(syncUiItems)

        initViewModel()

        underTest.state.test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that card click change the state to expanded`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        whenever(getSyncStalledIssuesUseCase()).thenReturn(emptyList())
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncFoldersState(syncUiItems.map { it.copy(expanded = true) })

        initViewModel()
        underTest.handleAction(
            SyncFoldersAction.CardExpanded(syncUiItems.first(), true)
        )

        underTest.state.test {
            Truth.assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that remove action removes folder pair`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val folderPairId = 9999L
        whenever(removeFolderPairUseCase(folderPairId)).thenReturn(Unit)

        initViewModel()
        underTest.handleAction(
            SyncFoldersAction.RemoveFolderClicked(folderPairId)
        )

        verify(removeFolderPairUseCase).invoke(folderPairId)
    }

    @Test
    fun `test that view model pause run click pauses sync if sync is not paused`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        val syncUiItem = getSyncUiItem(SyncStatus.SYNCING)
        initViewModel()

        underTest.handleAction(SyncFoldersAction.PauseRunClicked(syncUiItem))

        verify(pauseSyncUseCase).invoke(syncUiItem.id)
    }

    @Test
    fun `test that view model pause run clicked runs sync if sync is paused`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        val syncUiItem = getSyncUiItem(SyncStatus.PAUSED)
        initViewModel()

        underTest.handleAction(SyncFoldersAction.PauseRunClicked(syncUiItem))

        verify(resumeSyncUseCase).invoke(syncUiItem.id)
    }

    @Test
    fun `test that the folder is in error status when the stalled issues are not empty`() =
        runTest {
            whenever(monitorSyncsUseCase()).thenReturn(flow {
                emit(folderPairs)
                awaitCancellation()
            })
            whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
            whenever(getSyncStalledIssuesUseCase()).thenReturn(stalledIssues)
            val expectedState =
                SyncFoldersState(syncUiItems.map { it.copy(hasStalledIssues = true) })

            initViewModel()

            underTest.state.test {
                Truth.assertThat(awaitItem()).isEqualTo(expectedState)
            }
        }

    private fun getSyncUiItem(status: SyncStatus): SyncUiItem = SyncUiItem(
        id = 3L,
        folderPairName = "folderPair",
        status = status,
        deviceStoragePath = "DCIM",
        megaStoragePath = "photos",
        hasStalledIssues = false,
        method = R.string.sync_two_way,
        expanded = false
    )

    private fun initViewModel() {
        underTest = SyncFoldersViewModel(
            syncUiItemMapper,
            removeFolderPairUseCase,
            monitorSyncsUseCase,
            resumeSyncUseCase,
            pauseSyncUseCase,
            getSyncStalledIssuesUseCase
        )
    }
}