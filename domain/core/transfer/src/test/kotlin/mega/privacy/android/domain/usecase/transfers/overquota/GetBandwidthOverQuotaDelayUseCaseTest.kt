package mega.privacy.android.domain.usecase.transfers.overquota

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetBandwidthOverQuotaDelayUseCaseTest {

    private lateinit var underTest: GetBandwidthOverQuotaDelayUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetBandwidthOverQuotaDelayUseCase(transferRepository = transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that get banner quota time`() = runTest {
        val timer = 1000.seconds

        whenever(transferRepository.getBandwidthOverQuotaDelay()).thenReturn(timer)

        assertThat(underTest()).isEqualTo(timer)
    }
}