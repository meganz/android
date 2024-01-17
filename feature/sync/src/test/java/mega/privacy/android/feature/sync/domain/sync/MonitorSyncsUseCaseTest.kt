package mega.privacy.android.feature.sync.domain.sync

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
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.entity.RemoteFolder
import mega.privacy.android.feature.sync.domain.entity.SyncStatus
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorSyncsUseCaseTest {

    private val syncRepository: SyncRepository = mock()

    private val underTest = MonitorSyncsUseCase(
        syncRepository
    )

    private val folderPairs = listOf(
        FolderPair(
            3L,
            "folderPair",
            "DCIM",
            RemoteFolder(233L, "photos"),
            SyncStatus.SYNCING
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
            syncRepository,
        )
    }

    @Test
    fun `test that monitor folder pair changes emits flow of folder pairs`() = runTest {
        whenever(syncRepository.monitorFolderPairChanges()).thenReturn(
            flow {
                emit(folderPairs)
                awaitCancellation()
            },
        )

        underTest().test {
            val result = awaitItem()
            Truth.assertThat(result.size).isEqualTo(1)
            cancelAndIgnoreRemainingEvents()
        }

    }
}