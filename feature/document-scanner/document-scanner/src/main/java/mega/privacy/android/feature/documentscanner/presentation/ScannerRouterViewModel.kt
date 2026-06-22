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
import mega.privacy.android.feature.documentscanner.presentation.model.ScannerRoute
import timber.log.Timber
import javax.inject.Inject

/**
 * Resolves where to send the user when they enter the continuous scanner.
 *
 * Every entry point navigates to a single destination; this ViewModel runs the
 * launch-mode decision once and exposes a [ScannerRoute] the screen acts on —
 * camera, download flow, cellular-consent prompt, or legacy fallback.
 */
@HiltViewModel
class ScannerRouterViewModel @Inject constructor(
    private val getScannerLaunchMode: GetScannerLaunchModeUseCase,
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
}
