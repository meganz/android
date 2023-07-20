package mega.privacy.android.domain.usecase.transfer.activetransfers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddOrUpdateActiveTransferUseCaseTest {

    private lateinit var underTest: AddOrUpdateActiveTransferUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = AddOrUpdateActiveTransferUseCase(
            transferRepository = transferRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that invoke call insertOrUpdateActiveTransfer with the mapped transfer`(
        transferType: TransferType,
    ) = runTest {
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(transferType)
        }
        underTest.invoke(transfer)
        verify(transferRepository).insertOrUpdateActiveTransfer(transfer)
    }
}
