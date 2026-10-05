package mega.privacy.android.feature.sync.domain.sync.option

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.option.MonitorUserPausedSyncIdsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorUserPausedSyncIdsUseCaseTest {

    private lateinit var underTest: MonitorUserPausedSyncIdsUseCase

    private val syncPreferencesRepository = mock<SyncPreferencesRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorUserPausedSyncIdsUseCase(
            syncPreferencesRepository = syncPreferencesRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncPreferencesRepository)
    }

    @Test
    fun `test that invoke returns the repository flow`() = runTest {
        whenever(syncPreferencesRepository.monitorUserPausedSyncIds())
            .thenReturn(flowOf(setOf(1L), setOf(1L, 2L)))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(setOf(1L))
            assertThat(awaitItem()).isEqualTo(setOf(1L, 2L))
            awaitComplete()
        }
    }
}
