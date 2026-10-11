package mega.privacy.android.domain.usecase.transfers.overquota

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsInTransferOverQuotaUseCaseTest {

    private lateinit var underTest: IsInTransferOverQuotaUseCase

    private val transfersRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = IsInTransferOverQuotaUseCase(transfersRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transfersRepository)
    }

    @ParameterizedTest(name = " if get bandwidth over quota delay is {0}")
    @ValueSource(longs = [-1000, -500, -100, 0, 100, 500, 1000])
    fun `test that use case returns correctly`(
        delay: Long,
    ) = runTest {
        val delayAsDuration = delay.seconds
        val expected = delayAsDuration > 0.seconds

        whenever(transfersRepository.getBandwidthOverQuotaDelay()).thenReturn(delayAsDuration)

        assertThat(underTest()).isEqualTo(expected)
    }
}