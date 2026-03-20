package mega.privacy.android.app.presentation.whatsnew

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.bottomsheet.withBottomSheet
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.metadata.buildMetadata
import mega.privacy.android.navigation.destination.WhatsNewNavKey
import mega.privacy.mobile.home.presentation.whatsnew.WhatsNewUiState
import mega.privacy.mobile.home.presentation.whatsnew.WhatsNewViewModel

data object WhatsNewDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            entry<WhatsNewNavKey>(
                metadata = buildMetadata {
                    withBottomSheet(
                        dismissOnBack = false,
                        dismissOnOutsideClick = false
                    )
                }
            ) {
                val viewModel = hiltViewModel<WhatsNewViewModel>()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                (uiState as? WhatsNewUiState.Ready)
                    ?.whatsNewDetail
                    ?.screen(navigationHandler, onHandled)
            }
        }
}
