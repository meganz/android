package mega.privacy.android.feature.documentscanner.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import mega.android.core.ui.components.indicators.LargeInfiniteSpinnerIndicator
import mega.android.core.ui.tokens.theme.DSTokens
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
            ContinuousScanScreen(onClose = { navigationHandler.back() })

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
            onUseOldScanner = viewModel::onDownloadDeclined,
        )

        // TODO(AND-23986): replace PreparingDownload with the prepare/loading screen
        //  that observes WorkInfo. Until then, show a neutral preparing state.
        ScannerRoute.Resolving,
        ScannerRoute.PreparingDownload,
            -> {
            Timber.d("[DocScanner] Router showing loading for route: $current")
            LoadingState()
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DSTokens.colors.background.pageBackground),
        contentAlignment = Alignment.Center,
    ) {
        LargeInfiniteSpinnerIndicator()
    }
}
