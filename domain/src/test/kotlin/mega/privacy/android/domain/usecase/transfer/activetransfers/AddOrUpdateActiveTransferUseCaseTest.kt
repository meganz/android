package mega.privacy.android.domain.usecase.transfer.activetransfers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferMapper
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
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddOrUpdateActiveTransferUseCaseTest {

    private lateinit var underTest: AddOrUpdateActiveTransferUseCase

    private val transferRepository = mock<TransferRepository>()
    private val activeTransferMapper = mock<ActiveTransferMapper>()

    @BeforeAll
    fun setUp() {
        underTest = AddOrUpdateActiveTransferUseCase(
            transferRepository = transferRepository,
            activeTransferMapper = activeTransferMapper
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository, activeTransferMapper)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that invoke call insertOrUpdateActiveTransfer with the mapped transfer`(
        transferType: TransferType,
    ) = runTest {
        val original = mock<Transfer>()
        val expected = mock<ActiveTransfer>()
        whenever(activeTransferMapper(original)).thenReturn(expected)
        underTest.invoke(original)
        verify(transferRepository).insertOrUpdateActiveTransfer(expected)
    }
}
