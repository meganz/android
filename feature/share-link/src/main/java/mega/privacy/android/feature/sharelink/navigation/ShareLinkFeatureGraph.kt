package mega.privacy.android.feature.sharelink.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.sharelink.presentation.linkSettingsScreen
import mega.privacy.android.feature.sharelink.presentation.shareLinkScreen
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

/**
 * Registers the feature/share-link destinations (Share link + Link settings) into the
 * app's Navigation3 graph. Contributed `@IntoSet` from [mega.privacy.android.feature.sharelink.di.ShareLinkModule].
 */
class ShareLinkFeatureGraph : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, _ ->
            shareLinkScreen(navigationHandler = navigationHandler)
            linkSettingsScreen(navigationHandler = navigationHandler)
        }
}
