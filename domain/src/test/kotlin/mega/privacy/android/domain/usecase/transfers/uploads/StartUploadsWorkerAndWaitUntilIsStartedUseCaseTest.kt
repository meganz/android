package mega.privacy.android.domain.usecase.transfers.uploads

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
class StartUploadsWorkerAndWaitUntilIsStartedUseCaseTest {

    private lateinit var underTest: StartUploadsWorkerAndWaitUntilIsStartedUseCase

    private val startUploadsWorkerUseCase = mock<StartUploadsWorkerUseCase>()
    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = StartUploadsWorkerAndWaitUntilIsStartedUseCase(
            startUploadsWorkerUseCase = startUploadsWorkerUseCase,
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(startUploadsWorkerUseCase, transferRepository)

    @Test
    fun `test that startUploadsWorkerUseCase is invoked and suspend function returns when repository emits false in isUploadsWorkerEnqueuedFlow`() =
        runTest {
            val enqueuedFlow = MutableStateFlow(true)
            whenever(transferRepository.isUploadsWorkerEnqueuedFlow()).thenReturn(enqueuedFlow)
            launch {
                assertThat(enqueuedFlow.value).isTrue()
                underTest()
                verify(startUploadsWorkerUseCase).invoke()
                assertThat(enqueuedFlow.value).isFalse()
            }
            yield()
            enqueuedFlow.emit(false)
        }
}