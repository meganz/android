package mega.privacy.android.feature.sync.domain

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.FolderPairState
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairsUseCase
import mega.privacy.android.feature.sync.domain.usecase.MonitorSyncsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSyncsUseCaseTest {

    private val getFolderPairsUseCase: GetFolderPairsUseCase = mock()
    private val syncRepository: SyncRepository = mock()

    private val underTest = MonitorSyncsUseCase(
        getFolderPairsUseCase,
        syncRepository
    )

    private val folderPairs = listOf(
        FolderPair(
            3L,
            "folderPair",
            "DCIM",
            RemoteFolder(233L, "photos"),
            FolderPairState.RUNNING
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
            syncRepository,
        )
    }

    @Test
    fun `test that syncs are refetched every time syncs change`() = runTest {
        whenever(getFolderPairsUseCase()).thenReturn(folderPairs)
        whenever(syncRepository.monitorSyncChanges()).thenReturn(
            flow {
                emit(Unit)
                emit(Unit)
                awaitCancellation()
            })

        underTest().test { cancelAndIgnoreRemainingEvents() }

        verify(getFolderPairsUseCase, times(3)).invoke()
    }
}