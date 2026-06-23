package mega.privacy.android.app.presentation.documentscanner.legacy

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.core.nodecomponents.scanner.DocumentScanningError
import mega.privacy.android.core.nodecomponents.scanner.InsufficientRAMToLaunchDocumentScanner
import mega.privacy.android.core.nodecomponents.scanner.ScannerHandler
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel backing [LegacyScanDocumentLauncher].
 *
 * Always prepares the ML Kit document scanner, independent of the continuous-scanner
 * feature flag — this is the explicit legacy fallback reached via
 * `LegacyScanDocumentNavKey`, so it must never re-route to the new scanner the way
 * `ScanDocumentViewModel.prepareDocumentScanner()` does.
 *
 * The prepared scanner and any error are one-shot events: the UI launches the scanner
 * / shows the error once and then consumes it.
 */
@HiltViewModel
internal class LegacyScanDocumentViewModel @Inject constructor(
    private val scannerHandler: ScannerHandler,
) : ViewModel() {

    private val scannerReadyEventChannel =
        Channel<StateEventWithContent<GmsDocumentScanner>>(Channel.BUFFERED)
    private val scanningErrorEventChannel =
        Channel<StateEventWithContent<DocumentScanningError>>(Channel.BUFFERED)

    val uiState: StateFlow<LegacyScanDocumentUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            scannerReadyEventChannel.receiveAsFlow().onStart { emit(consumed()) },
            scanningErrorEventChannel.receiveAsFlow().onStart { emit(consumed()) },
        ) { scannerReadyEvent, scanningErrorEvent ->
            LegacyScanDocumentUiState(
                scannerReadyEvent = scannerReadyEvent,
                scanningErrorEvent = scanningErrorEvent,
            )
        }.asUiStateFlow(viewModelScope, LegacyScanDocumentUiState())
    }

    /** Prepares the ML Kit document scanner; the prepared scanner is launched by the UI. */
    fun prepareDocumentScanner() {
        viewModelScope.launch {
            runCatching { scannerHandler.prepareDocumentScanner() }
                .onSuccess { scanner -> scannerReadyEventChannel.send(triggered(scanner)) }
                .onFailure { exception ->
                    Timber.e(exception, "Failed to prepare the legacy document scanner")
                    scanningErrorEventChannel.send(triggered(exception.toScanningError()))
                }
        }
    }

    /** The scanner could not be opened after preparation (e.g. getStartScanIntent failed). */
    fun onDocumentScannerFailedToOpen() {
        scanningErrorEventChannel.trySend(triggered(DocumentScanningError.GenericError))
    }

    fun onScannerReadyEventConsumed() {
        scannerReadyEventChannel.trySend(consumed())
    }

    fun onScanningErrorEventConsumed() {
        scanningErrorEventChannel.trySend(consumed())
    }

    private fun Throwable.toScanningError() =
        if (this is InsufficientRAMToLaunchDocumentScanner) {
            DocumentScanningError.InsufficientRAM
        } else {
            DocumentScanningError.GenericError
        }
}

/**
 * @property scannerReadyEvent emitted with the prepared ML Kit scanner to launch.
 * @property scanningErrorEvent emitted with an error to surface in a dialog.
 */
@Stable
data class LegacyScanDocumentUiState(
    val scannerReadyEvent: StateEventWithContent<GmsDocumentScanner> = consumed(),
    val scanningErrorEvent: StateEventWithContent<DocumentScanningError> = consumed(),
)
