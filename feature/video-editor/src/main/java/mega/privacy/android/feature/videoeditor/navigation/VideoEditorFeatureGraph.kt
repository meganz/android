package mega.privacy.android.feature.videoeditor.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.feature.videoeditor.presentation.videoEditorScreen

class VideoEditorFeatureGraph : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit
        get() = { navigationHandler, transferHandler ->
            videoEditorScreen(
                navigationHandler = navigationHandler,
                transferHandler = transferHandler,
            )
        }
}
