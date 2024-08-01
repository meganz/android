package mega.privacy.android.feature.sync.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import mega.privacy.android.feature.sync.domain.usecase.sync.ClearSyncDebrisUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearSyncDebrisUseCaseTest {
    private lateinit var underTest: ClearSyncDebrisUseCase

    private val syncDebrisRepository = mock<SyncDebrisRepository>()

    @BeforeAll
    fun setup() {
        underTest = ClearSyncDebrisUseCase(
            syncDebrisRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(syncDebrisRepository)
    }

    @Test
    fun `test that clear sync debris use case invokes repository clear method`() = runTest {
        underTest()

        verify(syncDebrisRepository).clear()
    }
}