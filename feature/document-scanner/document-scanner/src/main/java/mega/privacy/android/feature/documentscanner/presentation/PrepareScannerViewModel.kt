package mega.privacy.android.feature.documentscanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState
import mega.privacy.android.feature.documentscanner.domain.usecase.MonitorScannerModelDownloadUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.StartScannerModelDownloadUseCase
import mega.privacy.android.feature.documentscanner.presentation.model.PrepareScannerUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * Drives the prepare/loading screen shown while the scanner model downloads.
 *
 * It observes the background download started by the router and exposes its
 * progress; the screen auto-navigates to the camera once the download completes.
 * [onRetryDownload] re-enqueues the download after a recoverable failure.
 */
@HiltViewModel
internal class PrepareScannerViewModel @Inject constructor(
    private val monitorScannerModelDownload: MonitorScannerModelDownloadUseCase,
    private val startScannerModelDownload: StartScannerModelDownloadUseCase,
) : ViewModel() {

    private val modelReadyChannel = Channel<StateEvent>(Channel.BUFFERED)

    val uiState: StateFlow<PrepareScannerUiState> by lazy(LazyThreadSafetyMode.NONE) {
        combine(
            monitorScannerModelDownload()
                .onEach { if (it is ScannerModelDownloadState.Completed) modelReadyChannel.trySend(triggered) },
            modelReadyChannel.receiveAsFlow().onStart { emit(consumed) },
        ) { downloadState, modelReadyEvent ->
            PrepareScannerUiState(downloadState = downloadState, modelReadyEvent = modelReadyEvent)
        }
            .catch { Timber.e(it, "[DocScanner] Failed to observe model download") }
            .asUiStateFlow(viewModelScope, PrepareScannerUiState())
    }

    fun onModelReadyConsumed() {
        modelReadyChannel.trySend(consumed)
    }

    /**
     * Re-enqueue the download after a recoverable failure. The unique-work policy
     * is KEEP, so this replaces the terminal failed job with a fresh attempt
     * without stacking duplicates.
     */
    fun onRetryDownload() {
        viewModelScope.launch {
            runCatching { startScannerModelDownload(requireUnmeteredNetwork = false) }
                .onFailure { Timber.e(it, "[DocScanner] Failed to re-enqueue model download on retry") }
        }
    }
}
