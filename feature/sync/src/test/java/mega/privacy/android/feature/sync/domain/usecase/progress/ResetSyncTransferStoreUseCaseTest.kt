package mega.privacy.android.feature.sync.domain.usecase.progress

import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.sync.domain.repository.SyncTransferStoreRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ResetSyncTransferStoreUseCaseTest {

    private lateinit var underTest: ResetSyncTransferStoreUseCase

    private val repository = mock<SyncTransferStoreRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ResetSyncTransferStoreUseCase(repository = repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke resets the store in the repository`() = runTest {
        underTest()

        verify(repository).reset()
    }
}
