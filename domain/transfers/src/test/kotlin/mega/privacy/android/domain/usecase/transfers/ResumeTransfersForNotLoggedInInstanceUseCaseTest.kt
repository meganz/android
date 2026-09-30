package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ResumeTransfersForNotLoggedInInstanceUseCaseTest {

    private lateinit var underTest: ResumeTransfersForNotLoggedInInstanceUseCase

    private val transfersRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = ResumeTransfersForNotLoggedInInstanceUseCase(transfersRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository)
    }

    @Test
    fun `test that use case invokes repository method`() = runTest {
        whenever(transfersRepository.resumeTransfersForNotLoggedInInstance()) doReturn Unit

        underTest()

        verify(transfersRepository).resumeTransfersForNotLoggedInInstance()
        verifyNoMoreInteractions(transfersRepository)
    }
}