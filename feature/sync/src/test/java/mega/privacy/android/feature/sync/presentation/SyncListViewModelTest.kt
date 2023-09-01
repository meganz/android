package mega.privacy.android.feature.sync.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.PauseSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairUseCase
import mega.privacy.android.feature.sync.domain.usecase.ResumeSyncUseCase
import mega.privacy.android.feature.sync.domain.usecase.SetOnboardingShownUseCase
import mega.privacy.android.feature.sync.ui.mapper.SyncUiItemMapper
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SyncListViewModelTest {

    private val syncUiItemMapper: SyncUiItemMapper = mock()
    private val removeFolderPairUseCase: RemoveFolderPairUseCase = mock()
    private val monitorSyncsUseCase: MonitorSyncsUseCase = mock()
    private val setOnboardingShownUseCase: SetOnboardingShownUseCase = mock()
    private val resumeSyncUseCase: ResumeSyncUseCase = mock()
    private val pauseSyncUseCase: PauseSyncUseCase = mock()

    private lateinit var underTest: SyncListViewModel

    private val folderPairs = listOf(
        FolderPair(
            3L,
            "folderPair",
            "DCIM",
            RemoteFolder(233L, "photos"),
            SyncStatus.SYNCING
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
            monitorSyncsUseCase,
            syncUiItemMapper,
            removeFolderPairUseCase,
            setOnboardingShownUseCase,
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
        whenever(syncUiItemMapper(folderPairs)).thenReturn(syncUiItems)
        val expectedState = SyncListState(syncUiItems)

        initViewModel()

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(expectedState)
        }
    }

    @Test
    fun `test that card click change the state to expanded`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
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
            SyncListAction.RemoveFolderClicked(folderPairId)
        )

        verify(removeFolderPairUseCase).invoke(folderPairId)
    }

    @Test
    fun `test that view model initialization sets onboarding shown to true`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        initViewModel()

        verify(setOnboardingShownUseCase).invoke(true)
    }

    @Test
    fun `test that view model pause run click pauses sync if sync is not paused`() = runTest {
        whenever(monitorSyncsUseCase()).thenReturn(flow {
            emit(folderPairs)
            awaitCancellation()
        })
        val syncUiItem = getSyncUiItem(SyncStatus.SYNCING)
        initViewModel()

        underTest.handleAction(SyncListAction.PauseRunClicked(syncUiItem))

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

        underTest.handleAction(SyncListAction.PauseRunClicked(syncUiItem))

        verify(resumeSyncUseCase).invoke(syncUiItem.id)
    }

    private fun getSyncUiItem(status: SyncStatus): SyncUiItem = SyncUiItem(
        id = 3L,
        folderPairName = "folderPair",
        status = status,
        deviceStoragePath = "DCIM",
        megaStoragePath = "photos",
        method = "Two-way sync",
        expanded = false
    )

    private fun initViewModel() {
        underTest = SyncListViewModel(
            syncUiItemMapper,
            removeFolderPairUseCase,
            monitorSyncsUseCase,
            setOnboardingShownUseCase,
            resumeSyncUseCase,
            pauseSyncUseCase
        )
    }
}