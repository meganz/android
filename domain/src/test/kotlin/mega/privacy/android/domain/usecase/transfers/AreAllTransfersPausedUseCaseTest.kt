package mega.privacy.android.domain.usecase.transfers

import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfer.AreAllTransfersPausedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

/**
 * Test class for [AreAllTransfersPausedUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreAllTransfersPausedUseCaseTest {

    private lateinit var underTest: AreAllTransfersPausedUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = AreAllTransfersPausedUseCase(
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest(name = "with {0} pending, {1} paused uploads and {2} paused downloads")
    @MethodSource("individualTransfersParameters")
    fun `test that true is returned when transfers are paused globally regardless of individual transfers`(
        pending: Int,
        pausedUploads: Int,
        pausedDownloads: Int,
        allPaused: Boolean,
    ) =
        runTest {
            val flow = MutableStateFlow(true)
            whenever(transferRepository.monitorPausedTransfers()).thenReturn(flow)
            whenever(transferRepository.getNumPendingTransfers()).thenReturn(pending)
            whenever(transferRepository.getNumPendingPausedUploads()).thenReturn(pausedUploads)
            whenever(transferRepository.getNumPendingNonBackgroundPausedDownloads()).thenReturn(
                pausedDownloads
            )
            val actual = underTest()
            assertThat(actual).isEqualTo(true)
        }

    @ParameterizedTest(name = "with {0} pending, {1} paused uploads and {2} paused downloads")
    @MethodSource("individualTransfersParameters")
    fun `test that expected is returned when transfers are not paused globally`(
        pending: Int,
        pausedUploads: Int,
        pausedDownloads: Int,
        allPaused: Boolean,
    ) =
        runTest {
            val flow = MutableStateFlow(false)
            whenever(transferRepository.monitorPausedTransfers()).thenReturn(flow)
            whenever(transferRepository.getNumPendingTransfers()).thenReturn(pending)
            whenever(transferRepository.getNumPendingPausedUploads()).thenReturn(pausedUploads)
            whenever(transferRepository.getNumPendingNonBackgroundPausedDownloads()).thenReturn(
                pausedDownloads
            )
            val actual = underTest()
            assertThat(actual).isEqualTo(allPaused)
        }

    private fun individualTransfersParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(10, 0, 0, false),
        Arguments.of(10, 5, 0, false),
        Arguments.of(10, 5, 5, true),
        Arguments.of(10, 10, 0, true),
        Arguments.of(10, 0, 10, true),
        Arguments.of(0, 0, 0, false),
    )
}
