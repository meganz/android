package mega.privacy.android.feature.photos.navigation

import androidx.navigation3.runtime.EntryProviderBuilder
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler

class PhotosFeatureGraph : FeatureDestination {
    override val navigationGraph: EntryProviderBuilder<NavKey>.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, transferHandler ->

        }
}