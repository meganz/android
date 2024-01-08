package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnsureDownloadsWorkerHasStartedUseCaseTest {
    private lateinit var underTest: EnsureDownloadsWorkerHasStartedUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = EnsureDownloadsWorkerHasStartedUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() =
        reset(transferRepository)

    @Test
    fun `test that suspend function returns when repository emits false in isDownloadsWorkerEnqueuedFlow`() =
        runTest {
            val enqueuedFlow = MutableStateFlow(true)
            whenever(transferRepository.isDownloadsWorkerEnqueuedFlow()).thenReturn(enqueuedFlow)
            launch {
                assertThat(enqueuedFlow.value).isTrue()
                underTest()
                assertThat(enqueuedFlow.value).isFalse()
            }
            yield()
            enqueuedFlow.emit(false)
        }
}