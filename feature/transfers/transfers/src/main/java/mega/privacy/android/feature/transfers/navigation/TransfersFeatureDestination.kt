package mega.privacy.android.feature.transfers.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.transfers.presentation.settings.TransfersSettingsViewModel
import mega.privacy.android.feature.transfers.presentation.settings.view.TransfersSettingsView
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.destination.TransfersSettingsNavKey

class TransfersFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, _ ->
            transfersSettingsDestination { navigationHandler.remove(it) }
        }

    fun EntryProviderScope<NavKey>.transfersSettingsDestination(
        onNavigateBack: (NavKey) -> Unit,
    ) {
        entry<TransfersSettingsNavKey> { key ->
            val viewModel = hiltViewModel<TransfersSettingsViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            TransfersSettingsView(
                uiState = uiState,
                onSetMaxDownloadConnections = viewModel::setMaxDownloadConnections,
                onSetMaxUploadConnections = viewModel::setMaxUploadConnections,
                onNavigateBack = { onNavigateBack(key) }
            )
        }
    }
}