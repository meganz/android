package mega.privacy.android.app.activities.destinations

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryFragment
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.MediaDiscoveryNavKey

fun NavGraphBuilder.legacyMediaDiscoveryScreen(
    navigationHandler: NavigationHandler,
) {
    composable<MediaDiscoveryNavKey> { backStackEntry ->
        val args = backStackEntry.toRoute<MediaDiscoveryNavKey>()

        AndroidFragment<MediaDiscoveryFragment>(
            arguments = bundleOf(
                MediaDiscoveryFragment.INTENT_KEY_CURRENT_FOLDER_ID to args.nodeHandle,
                MediaDiscoveryFragment.INTENT_KEY_CURRENT_FOLDER_NAME to args.nodeName,
                MediaDiscoveryFragment.IS_NEW_DESIGN to true,
            ),
            modifier = Modifier.systemBarsPadding(),
        ) { fragment ->
            fragment.navigationHandler = navigationHandler
        }
    }
}