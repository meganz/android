package mega.privacy.android.feature.documentscanner.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.feature.documentscanner.presentation.ScannerRouterViewModel
import mega.privacy.android.feature.documentscanner.presentation.component.ScannerDownloadConfirmationDialog
import mega.privacy.android.feature.documentscanner.presentation.model.ScannerRoute
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.navkey.ContinuousScanNavKey
import mega.privacy.android.navigation.contract.navkey.LegacyScanDocumentNavKey
import timber.log.Timber

/**
 * Entry point for the continuous scanner. Resolves the launch mode and renders /
 * navigates accordingly: the camera, the download flow, or the legacy fallback.
 *
 * Every scan entry point lands here (via [ContinuousScanNavKey]) so the routing
 * decision is made in exactly one place.
 *
 * @param navigationHandler used to fall back to the legacy ML Kit scanner.
 */
@Composable
internal fun ScannerRouterScreen(
    navigationHandler: NavigationHandler,
    viewModel: ScannerRouterViewModel = hiltViewModel(),
) {
    val route by viewModel.route.collectAsStateWithLifecycle()

    when (val current = route) {
        ScannerRoute.LaunchCamera ->
            ContinuousScanScreen(
                onClose = { navigationHandler.back() },
                onSwitchToLegacy = {
                    navigationHandler.navigate(LegacyScanDocumentNavKey)
                    navigationHandler.remove(ContinuousScanNavKey)
                },
            )

        is ScannerRoute.UseLegacy -> LaunchedEffect(current) {
            // The legacy ML Kit scanner is launched by an app-shell handler that
            // owns the ActivityResultLauncher and services this key.
            // TODO(AND-23987): handler lands with the global legacy-scan launch ticket.
            navigationHandler.navigate(LegacyScanDocumentNavKey)
            navigationHandler.remove(ContinuousScanNavKey)
        }

        ScannerRoute.NeedsDownload -> ScannerDownloadConfirmationDialog(
            onCellular = false,
            onConfirmDownload = viewModel::onDownloadConfirmed,
            onUseOldScanner = viewModel::onDownloadDeclined,
        )

        ScannerRoute.NeedsCellularConsent -> ScannerDownloadConfirmationDialog(
            onCellular = true,
            onConfirmDownload = viewModel::onCellularDownloadConfirmed,
            onUseOldScanner = viewModel::onCellularDownloadDeclined,
        )

        ScannerRoute.PreparingDownload -> PrepareScannerScreen(
            onModelReady = viewModel::onModelReady,
            onUseLegacy = viewModel::onPrepareUseLegacy,
            onClose = { navigationHandler.back() },
        )

        // Launch-mode resolution is near-instant. Render a transparent, touch-blocking
        // placeholder rather than an opaque loading screen: an opaque fill would flash
        // before the confirmation dialog dims the screen behind this transparent entry.
        ScannerRoute.Resolving -> {
            Timber.d("[DocScanner] Router resolving route: $current")
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blockTouchPassThrough(),
            )
        }
    }
}
