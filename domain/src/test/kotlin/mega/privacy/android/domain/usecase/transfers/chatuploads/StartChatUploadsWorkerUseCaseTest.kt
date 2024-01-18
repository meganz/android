package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartChatUploadsWorkerUseCaseTest {

    private lateinit var underTest: StartChatUploadsWorkerUseCase

    private lateinit var transferRepository: TransferRepository

    @BeforeAll
    fun setup() {
        transferRepository = mock()
        underTest = StartChatUploadsWorkerUseCase(transferRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that StartChatUploadsWorkerUseCase invokes startChatUploadsWorker in TransferRepository`() =
        runTest {
            underTest.invoke()
            verify(transferRepository).startChatUploadsWorker()
            verifyNoMoreInteractions(transferRepository)
        }
}