package mega.privacy.android.domain.usecase.transfer.activetransfers

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorActiveTransfersUseCaseTest {

    private lateinit var underTest: MonitorActiveTransfersUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorActiveTransfersUseCase(
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that invoke returns transferRepository active transfers`(
        transferType: TransferType,
    ) {
        val expected = mock<Flow<List<ActiveTransfer>>>()
        whenever(transferRepository.getActiveTransfersByType(transferType)).thenReturn(expected)
        Truth.assertThat(underTest.invoke(transferType)).isEqualTo(expected)
    }
}
