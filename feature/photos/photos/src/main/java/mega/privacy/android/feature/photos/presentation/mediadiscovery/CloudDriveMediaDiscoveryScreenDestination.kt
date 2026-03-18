package mega.privacy.android.feature.photos.presentation.mediadiscovery

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import mega.privacy.android.core.nodecomponents.sheet.options.NodeOptionsBottomSheetNavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.CloudDriveMediaDiscoveryNavKey

/**
 * Entry for Cloud Drive Media Discovery Screen
 * @param navigationHandler Navigation handler to handle navigation actions
 * @param setNavigationBarVisibility Optional callback to set the visibility of the bottom navigation bar
 */
fun EntryProviderScope<NavKey>.cloudDriveMediaDiscoveryScreen(
    navigationHandler: NavigationHandler,
) {
    entry<CloudDriveMediaDiscoveryNavKey>(
        metadata = NavDisplay.transitionSpec {
            EnterTransition.None togetherWith ExitTransition.None
        }
    ) { key ->
        val viewModel =
            hiltViewModel<CloudDriveMediaDiscoveryViewModel, CloudDriveMediaDiscoveryViewModel.Factory>(
                creationCallback = {
                    it.create(
                        folderId = key.folderId,
                        folderName = key.folderName,
                        fromFolderLink = key.fromFolderLink,
                    )
                }
            )

        CloudDriveMediaDiscoveryRoute(
            viewModel = viewModel,
            onBack = navigationHandler::back,
            onMoreOptionsClicked = {
                if (key.folderId != -1L) {
                    navigationHandler.navigate(
                        NodeOptionsBottomSheetNavKey(
                            nodeHandle = key.folderId,
                            nodeSourceType = key.nodeSourceType,
                        )
                    )
                }
            },
        )
    }
}
