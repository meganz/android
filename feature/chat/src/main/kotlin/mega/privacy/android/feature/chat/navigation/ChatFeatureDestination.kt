package mega.privacy.android.feature.chat.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey

class ChatFeatureDestination : FeatureDestination {
    override val navigationGraph: EntryProviderScope<NavKey>.(navigationHandler: NavigationHandler, transferHandler: TransferHandler) -> Unit =
        { navigationHandler, transferHandler ->

        }
}

data object ChatDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            meetingHasEndedDialog(
                navigateBack = navigationHandler::back,
                navigate = navigationHandler::navigate,
                onDialogHandled = onHandled
            )
            callRecordingConsentDialog(
                navigate = navigationHandler::navigate,
                onHandled = onHandled,
                remove = navigationHandler::remove
            )
        }
}