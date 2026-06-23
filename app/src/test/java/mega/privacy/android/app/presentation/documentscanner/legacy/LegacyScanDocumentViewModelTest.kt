package mega.privacy.android.app.presentation.documentscanner.legacy

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class LegacyScanDocumentViewModelTest {

    private lateinit var underTest: LegacyScanDocumentViewModel

    private val scannerHandler = mock<ScannerHandler>()

    @BeforeEach
    fun setUp() {
        reset(scannerHandler)
        underTest = LegacyScanDocumentViewModel(scannerHandler)
    }

    @Test
    fun `test that prepareDocumentScanner triggers the scanner ready event on success`() = runTest {
        val scanner = mock<GmsDocumentScanner>()
        whenever(scannerHandler.prepareDocumentScanner()).thenReturn(scanner)

        underTest.uiState.test {
            awaitItem() // initial (consumed)
            underTest.prepareDocumentScanner()
            val event = awaitItem().scannerReadyEvent
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((event as StateEventWithContentTriggered).content).isEqualTo(scanner)
        }
    }

    @Test
    fun `test that prepareDocumentScanner triggers InsufficientRAM error when ram is insufficient`() =
        runTest {
            whenever(scannerHandler.prepareDocumentScanner())
                .thenAnswer { throw InsufficientRAMToLaunchDocumentScanner() }

            underTest.uiState.test {
                awaitItem()
                underTest.prepareDocumentScanner()
                val event = awaitItem().scanningErrorEvent
                assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
                assertThat((event as StateEventWithContentTriggered).content)
                    .isEqualTo(DocumentScanningError.InsufficientRAM)
            }
        }

    @Test
    fun `test that prepareDocumentScanner triggers a generic error on other failures`() = runTest {
        whenever(scannerHandler.prepareDocumentScanner())
            .thenAnswer { throw RuntimeException("boom") }

        underTest.uiState.test {
            awaitItem()
            underTest.prepareDocumentScanner()
            val event = awaitItem().scanningErrorEvent
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((event as StateEventWithContentTriggered).content)
                .isEqualTo(DocumentScanningError.GenericError)
        }
    }

    @Test
    fun `test that onDocumentScannerFailedToOpen triggers a generic error`() = runTest {
        underTest.uiState.test {
            awaitItem()
            underTest.onDocumentScannerFailedToOpen()
            val event = awaitItem().scanningErrorEvent
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            assertThat((event as StateEventWithContentTriggered).content)
                .isEqualTo(DocumentScanningError.GenericError)
        }
    }

    @Test
    fun `test that onScannerReadyEventConsumed clears the scanner ready event`() = runTest {
        val scanner = mock<GmsDocumentScanner>()
        whenever(scannerHandler.prepareDocumentScanner()).thenReturn(scanner)

        underTest.uiState.test {
            awaitItem()
            underTest.prepareDocumentScanner()
            assertThat(awaitItem().scannerReadyEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.onScannerReadyEventConsumed()
            assertThat(awaitItem().scannerReadyEvent)
                .isNotInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }

    @Test
    fun `test that onScanningErrorEventConsumed clears the error event`() = runTest {
        underTest.uiState.test {
            awaitItem()
            underTest.onDocumentScannerFailedToOpen()
            assertThat(awaitItem().scanningErrorEvent)
                .isInstanceOf(StateEventWithContentTriggered::class.java)

            underTest.onScanningErrorEventConsumed()
            assertThat(awaitItem().scanningErrorEvent)
                .isNotInstanceOf(StateEventWithContentTriggered::class.java)
        }
    }
}
