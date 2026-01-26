package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

/**
 * Test class for [DeleteActiveTransferGroupUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteActiveTransferGroupUseCaseTest {

    private lateinit var underTest: DeleteActiveTransferGroupUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteActiveTransferGroupUseCase(
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that deleteActiveTransferGroup is called with correct groupId`() = runTest {
        val groupId = 123

        underTest(groupId)

        verify(transferRepository).deleteActiveTransferGroup(groupId)
        verifyNoMoreInteractions(transferRepository)
    }
}
