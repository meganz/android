package mega.privacy.android.feature.sync.domain.usecase.progress

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.entity.progress.SyncTransferStoreState
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorSyncTransferStoreUseCaseTest {

    private lateinit var underTest: MonitorSyncTransferStoreUseCase

    private val repository = mock<SyncTransferStoreRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorSyncTransferStoreUseCase(repository = repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke returns the repository store flow`() = runTest {
        whenever(repository.monitorSyncTransferStore())
            .thenReturn(flowOf(SyncTransferStoreState.NotSeeded))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(SyncTransferStoreState.NotSeeded)
            awaitComplete()
        }
    }
}
