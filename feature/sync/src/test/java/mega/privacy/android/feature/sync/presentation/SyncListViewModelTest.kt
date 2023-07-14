package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.ui.mapper.SyncUiItemMapper
import mega.privacy.android.feature.sync.ui.model.SyncStatus
import mega.privacy.android.feature.sync.ui.model.SyncUiItem
import mega.privacy.android.feature.sync.ui.synclist.SyncListAction
import mega.privacy.android.feature.sync.ui.synclist.SyncListState
import mega.privacy.android.feature.sync.ui.synclist.SyncListViewModel
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncListViewModelTest {

    private val getFolderPairsUseCase: GetFolderPairsUseCase = mock()
    private val syncUiItemMapper: SyncUiItemMapper = mock()

    private lateinit var underTest: SyncListViewModel

    private val folderPairs = listOf(
        FolderPair(
            3L,
            "folderPair",
            "DCIM",
            RemoteFolder(233L, "photos"),
            FolderPairState.RUNNING
        )
    )

    private val syncUiItems = listOf(
        SyncUiItem(
            id = 3L,
            folderPairName = "folderPair",
            status = SyncStatus.SYNCING,
            deviceStoragePath = "DCIM",
            megaStoragePath = "photos",
            method = "Two-way sync",
            expanded = false
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
            getFolderPairsUseCase,
            syncUiItemMapper,
        )
    }

    @Test
    fun `test that viewmodel fetches all folder pairs upon initialization`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncListState(syncUiItems)

        initViewModel()

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that card click change the state to expanded`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncListState(syncUiItems.map { it.copy(expanded = true) })

        initViewModel()
        underTest.handleAction(
            SyncListAction.CardExpanded(syncUiItems.first(), true)
        )

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    private fun initViewModel() {
        underTest = SyncListViewModel(getFolderPairsUseCase, syncUiItemMapper)
    }
}