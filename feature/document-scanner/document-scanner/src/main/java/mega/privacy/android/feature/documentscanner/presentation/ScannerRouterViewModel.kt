package mega.privacy.android.feature.documentscanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mega.privacy.android.feature.documentscanner.domain.launchmode.CellularConsentRequiredException
import mega.privacy.android.feature.documentscanner.domain.launchmode.LegacyReason
import mega.privacy.android.feature.documentscanner.domain.launchmode.ScannerLaunchMode
import mega.privacy.android.feature.documentscanner.domain.usecase.GetScannerLaunchModeUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.GrantScannerCellularConsentUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.StartScannerModelDownloadUseCase
import mega.privacy.android.feature.documentscanner.presentation.model.ScannerRoute
import timber.log.Timber
import javax.inject.Inject

/**
 * Resolves where to send the user when they enter the continuous scanner.
 *
 * Every entry point navigates to a single destination; this ViewModel runs the
 * launch-mode decision once and exposes a [ScannerRoute] the screen acts on —
 * camera, download flow, cellular-consent prompt, or legacy fallback. The
 * confirmation dialog's choices feed back through [onDownloadConfirmed],
 * [onCellularDownloadConfirmed], [onDownloadDeclined], and
 * [onCellularDownloadDeclined], which also start the model download with the
 * appropriate network constraint.
 */
@HiltViewModel
class ScannerRouterViewModel @Inject constructor(
    private val getScannerLaunchMode: GetScannerLaunchModeUseCase,
    private val grantScannerCellularConsent: GrantScannerCellularConsentUseCase,
    private val startScannerModelDownload: StartScannerModelDownloadUseCase,
) : ViewModel() {

    private val _route = MutableStateFlow<ScannerRoute>(ScannerRoute.Resolving)
    val route: StateFlow<ScannerRoute> = _route.asStateFlow()

    init {
        resolveRoute()
    }

    private fun resolveRoute() {
        viewModelScope.launch {
            _route.value = runCatching { getScannerLaunchMode() }.fold(
                onSuccess = { mode ->
                    when (mode) {
                        ScannerLaunchMode.New -> ScannerRoute.LaunchCamera
                        ScannerLaunchMode.NeedsDownload -> ScannerRoute.NeedsDownload
                        is ScannerLaunchMode.Legacy -> ScannerRoute.UseLegacy(mode.reason)
                    }
                },
                onFailure = { error ->
                    if (error is CellularConsentRequiredException) {
                        ScannerRoute.NeedsCellularConsent
                    } else {
                        Timber.e(error, "[DocScanner] Launch-mode resolution failed; using legacy")
                        ScannerRoute.UseLegacy(LegacyReason.Unknown)
                    }
                },
            )
        }
    }

    /**
     * The user confirmed the download on Wi-Fi (or on cellular with consent already
     * granted). Start the download now on any connected network, then hand off to the
     * prepare/loading screen that observes it.
     */
    fun onDownloadConfirmed() {
        viewModelScope.launch {
            enqueueModelDownload(requireUnmeteredNetwork = false)
            _route.value = ScannerRoute.PreparingDownload
        }
    }

    /**
     * The user confirmed the download over cellular. Persist the metered-data consent
     * so it is never asked again, start the download now, then hand off to the
     * prepare/loading screen.
     */
    fun onCellularDownloadConfirmed() {
        viewModelScope.launch {
            runCatching { grantScannerCellularConsent() }
                .onFailure { Timber.e(it, "[DocScanner] Failed to persist cellular consent") }
            enqueueModelDownload(requireUnmeteredNetwork = false)
            _route.value = ScannerRoute.PreparingDownload
        }
    }

    /**
     * The user declined the download on Wi-Fi; fall back to legacy without downloading.
     */
    fun onDownloadDeclined() {
        _route.value = ScannerRoute.UseLegacy(LegacyReason.UserDeclined)
    }

    /**
     * The user declined the metered download; defer the download to an un-metered
     * network so it completes in the background, and fall back to legacy meanwhile.
     */
    fun onCellularDownloadDeclined() {
        viewModelScope.launch {
            enqueueModelDownload(requireUnmeteredNetwork = true)
            _route.value = ScannerRoute.UseLegacy(LegacyReason.UserDeclined)
        }
    }

    private suspend fun enqueueModelDownload(requireUnmeteredNetwork: Boolean) {
        runCatching { startScannerModelDownload(requireUnmeteredNetwork) }
            .onFailure { Timber.e(it, "[DocScanner] Failed to enqueue model download") }
    }
}
