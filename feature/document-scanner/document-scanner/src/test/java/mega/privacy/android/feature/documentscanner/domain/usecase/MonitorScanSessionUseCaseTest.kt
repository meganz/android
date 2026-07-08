package mega.privacy.android.feature.documentscanner.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.ScanSession
import mega.privacy.android.feature.documentscanner.domain.repository.ScanSessionRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorScanSessionUseCaseTest {

    private val scanSessionRepository = mock<ScanSessionRepository>()
    private lateinit var underTest: MonitorScanSessionUseCase

    @BeforeEach
    fun setUp() {
        reset(scanSessionRepository)
        underTest = MonitorScanSessionUseCase(scanSessionRepository)
    }

    @Test
    fun `test that invoke returns the repository session flow`() = runTest {
        val session = ScanSession(
            id = "session",
            pages = emptyList(),
            captureMode = CaptureMode.AUTO,
            createdAt = 0L,
        )
        whenever(scanSessionRepository.getSession()).thenReturn(flowOf(session))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(session)
            awaitComplete()
        }
    }
}
