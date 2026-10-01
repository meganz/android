package mega.privacy.android.domain.usecase.transfers.overquota

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTransferOverQuotaUseCaseTest {

    private lateinit var underTest: MonitorTransferOverQuotaUseCase

    private val transferRepository = mock<TransferRepository>()
    private val isInTransferOverQuotaUseCase = mock<IsInTransferOverQuotaUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorTransferOverQuotaUseCase(
            transferRepository = transferRepository,
            isInTransferOverQuotaUseCase = isInTransferOverQuotaUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository, isInTransferOverQuotaUseCase)
    }

    @Test
    fun `test that use case returns correctly`() = runTest {
        val flow = MutableStateFlow(false)

        whenever(transferRepository.monitorTransferOverQuota()).thenReturn(flow)
        whenever(isInTransferOverQuotaUseCase()).thenReturn(false)

        underTest().test {
            assertThat(awaitItem()).isFalse()
            flow.emit(true)
            assertThat(awaitItem()).isTrue()
        }
    }
}