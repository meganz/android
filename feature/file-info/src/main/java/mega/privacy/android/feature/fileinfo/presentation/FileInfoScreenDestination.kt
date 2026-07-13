package mega.privacy.android.feature.fileinfo.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.navOptions
import mega.privacy.android.navigation.destination.FileInfoNavKey
import mega.privacy.android.navigation.destination.LegacyFileInfoNavKey

fun EntryProviderScope<NavKey>.fileInfoScreen(
    navigationHandler: NavigationHandler,
    transferHandler: TransferHandler,
) {
    entry<FileInfoNavKey> { key ->
        FeatureFlagGate(
            feature = ApiFeatures.FileInfoRevamp,
            disabled = {
                LaunchedEffect(Unit) {
                    navigationHandler.remove(key)
                    navigationHandler.navigate(LegacyFileInfoNavKey(key.nodeHandle))
                }
            }
        ) {
            val viewModel = hiltViewModel<FileInfoViewModel, FileInfoViewModel.Factory> { factory ->
                factory.create(key.nodeHandle)
            }
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            FileInfoScreen(
                uiState = uiState,
                nodeHandle = key.nodeHandle,
                onBack = navigationHandler::back,
                onLocationClick = {
                    uiState.locationDestinations?.let { destinations ->
                        // Close File Info before opening the folder so the back stack
                        // doesn't loop folder -> file -> info -> folder.
                        navigationHandler.remove(key)
                        navigationHandler.navigate(
                            destinations = destinations,
                            navOptions = navOptions { launchSingleTop = true },
                        )
                    }
                },
                onNavigate = { navigationHandler.navigate(it) },
                onDescriptionChange = viewModel::updateDescription,
            )
        }
    }
}
