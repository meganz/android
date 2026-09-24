package mega.privacy.android.shared.nodes.namecollision.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.shared.nodes.namecollision.NameCollisionLaunchSourceHolder
import javax.inject.Inject

/**
 * [FeatureDestination] contributing the Compose name-collision screen
 * ([mega.privacy.android.navigation.destination.NameCollisionNavKey]) to the single-activity
 * navigation graph.
 */
class NameCollisionFeatureDestination @Inject constructor(
    private val launchSourceHolder: NameCollisionLaunchSourceHolder,
) : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->
            nameCollisionScreen(
                navigationHandler = navigationHandler,
                transferHandler = transferHandler,
                launchSourceHolder = launchSourceHolder,
            )
        }
}
