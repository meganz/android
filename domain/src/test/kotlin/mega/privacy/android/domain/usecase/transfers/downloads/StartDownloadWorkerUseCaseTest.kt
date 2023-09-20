package mega.privacy.android.domain.usecase.transfers.downloads

import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartDownloadWorkerUseCaseTest {

    private lateinit var underTest: StartDownloadWorkerUseCase

    private lateinit var transferRepository: TransferRepository

    @BeforeAll
    fun setup() {
        transferRepository = mock()
        underTest = StartDownloadWorkerUseCase(transferRepository)
    }

    @AfterAll
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that StartDownloadWorkerUseCase invokes startDownloadWorker in TransferRepository`() =
        runTest {
            underTest.invoke()
            verify(transferRepository).startDownloadWorker()
            verifyNoMoreInteractions(transferRepository)
        }
}