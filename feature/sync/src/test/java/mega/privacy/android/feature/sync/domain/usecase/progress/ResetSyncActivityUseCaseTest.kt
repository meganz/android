package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncProgressRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResetSyncActivityUseCaseTest {

    private lateinit var underTest: ResetSyncActivityUseCase

    private val syncProgressRepository = mock<SyncProgressRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ResetSyncActivityUseCase(syncProgressRepository = syncProgressRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncProgressRepository)
    }

    @Test
    fun `test that invoke resets the sync activity in the repository`() = runTest {
        underTest()

        verify(syncProgressRepository).resetSyncActivity()
    }
}
