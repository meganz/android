package mega.privacy.android.domain.usecase.transfers.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorInProgressTransfersUseCaseTest {

    private lateinit var underTest: MonitorInProgressTransfersUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorInProgressTransfersUseCase(
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that invoke returns transferRepository in progress transfers`() {
        val expected = mock<Flow<Map<Int, InProgressTransfer>>>()
        whenever(transferRepository.monitorInProgressTransfers()).thenReturn(expected)
        Truth.assertThat(underTest.invoke()).isEqualTo(expected)
    }
}
