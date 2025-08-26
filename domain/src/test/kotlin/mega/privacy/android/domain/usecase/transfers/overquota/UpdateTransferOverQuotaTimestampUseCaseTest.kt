package mega.privacy.android.domain.usecase.transfers.overquota

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.environment.GetCurrentTimeInMillisUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.concurrent.atomic.AtomicLong

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateTransferOverQuotaTimestampUseCaseTest {

    private lateinit var underTest: UpdateTransferOverQuotaTimestampUseCase

    private val transfersRepository = mock<TransferRepository>()
    private val getCurrentTimeInMillisUseCase = mock<GetCurrentTimeInMillisUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateTransferOverQuotaTimestampUseCase(
            transferRepository = transfersRepository,
            getCurrentTimeInMillisUseCase = getCurrentTimeInMillisUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository, getCurrentTimeInMillisUseCase)
    }

    @Test
    fun `test that invoking the use case updates the timestamp in the repository`() {
        val currentTime = 123456789L
        val atomicLong = AtomicLong(123L)

        whenever(transfersRepository.transferOverQuotaTimestamp) doReturn atomicLong
        whenever(getCurrentTimeInMillisUseCase()) doReturn currentTime

        underTest()

        assertThat(atomicLong.get()).isEqualTo(currentTime)
    }
}