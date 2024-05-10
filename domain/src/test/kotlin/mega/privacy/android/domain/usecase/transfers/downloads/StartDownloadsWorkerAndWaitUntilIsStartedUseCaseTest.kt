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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartDownloadsWorkerAndWaitUntilIsStartedUseCaseTest {

    private lateinit var underTest: StartDownloadsWorkerAndWaitUntilIsStartedUseCase

    private val startDownloadWorkerUseCase = mock<StartDownloadWorkerUseCase>()
    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = StartDownloadsWorkerAndWaitUntilIsStartedUseCase(
            startDownloadWorkerUseCase = startDownloadWorkerUseCase,
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(startDownloadWorkerUseCase, transferRepository)

    @Test
    fun `test that startDownloadWorkerUseCase is invoked and suspend function returns when repository emits false in isDownloadsWorkerEnqueuedFlow`() =
        runTest {
            val enqueuedFlow = MutableStateFlow(true)
            whenever(transferRepository.isDownloadsWorkerEnqueuedFlow()).thenReturn(enqueuedFlow)
            launch {
                assertThat(enqueuedFlow.value).isTrue()
                underTest()
                verify(startDownloadWorkerUseCase).invoke()
                assertThat(enqueuedFlow.value).isFalse()
            }
            yield()
            enqueuedFlow.emit(false)
        }
}