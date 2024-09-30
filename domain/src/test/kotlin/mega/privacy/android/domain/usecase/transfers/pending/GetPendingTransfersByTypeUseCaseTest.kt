package mega.privacy.android.domain.usecase.transfers.pending

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPendingTransfersByTypeUseCaseTest {

    private lateinit var underTest: GetPendingTransfersByTypeUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetPendingTransfersByTypeUseCase(transferRepository)
    }

    @BeforeEach
    fun cleanUp() = reset(transferRepository)

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that repository result is returned when the use case is invoked`(transferType: TransferType) =
        runTest {
            val expected = mock<Flow<List<PendingTransfer>>>()
            whenever(transferRepository.getPendingTransfersByType(transferType)) doReturn expected
            val actual = underTest.invoke(transferType)
            assertThat(actual).isEqualTo(expected)
        }
}
