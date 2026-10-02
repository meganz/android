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
internal class ClearCompletedSyncTransferUseCaseTest {

    private lateinit var underTest: ClearCompletedSyncTransferUseCase

    private val repository = mock<SyncTransferStoreRepository>()

    @BeforeAll
    fun setUp() {
        underTest = ClearCompletedSyncTransferUseCase(repository = repository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(repository)
    }

    @Test
    fun `test that invoke clears the tag in the repository`() = runTest {
        underTest(tag = 7)

        verify(repository).clearCompleted(setOf(7))
    }
}
