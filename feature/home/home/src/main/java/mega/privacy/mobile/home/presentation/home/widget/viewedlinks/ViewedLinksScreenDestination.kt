package mega.privacy.mobile.home.presentation.home.widget.viewedlinks

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.FolderLinkNavKey
import mega.privacy.android.navigation.destination.LegacyFileLinkNavKey
import mega.privacy.android.navigation.destination.ViewedLinksScreenNavKey

/**
 * Navigation destination for the full-screen Viewed Links list.
 */
fun EntryProviderScope<NavKey>.viewedLinksScreen(
    navigationHandler: NavigationHandler
) {
    entry<ViewedLinksScreenNavKey> {
        val viewModel = hiltViewModel<ViewedLinksViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        ViewedLinksScreen(
            uiState = uiState,
            onFolderLinkClicked = { link ->
                navigationHandler.navigate(FolderLinkNavKey(link))
            },
            onFileLinkClicked = { link ->
                navigationHandler.navigate(LegacyFileLinkNavKey(link))
            },
            onBack = navigationHandler::back,
        )
    }
}
