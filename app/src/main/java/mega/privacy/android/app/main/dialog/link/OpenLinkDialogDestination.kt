package mega.privacy.android.app.main.dialog.link

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.core.nodecomponents.dialog.openlink.OpenLinkDialog
import mega.privacy.android.navigation.contract.NavOptions
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.DeepLinksDialogNavKey
import mega.privacy.android.navigation.destination.OpenLinkDialogNavKey

data object OpenLinkDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            openLinkDialogDestination(
                remove = navigationHandler::remove,
                navigate = { destinations, navOptions ->
                    navigationHandler.navigate(
                        destinations = destinations,
                        navOptions = navOptions
                    )
                },
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.openLinkDialogDestination(
    remove: (NavKey) -> Unit,
    navigate: (List<NavKey>, NavOptions?) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<OpenLinkDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        OpenLinkDialog(
            onOpenLink = { link ->
                remove(key)
                onDialogHandled()
                navigate(
                    listOf(DeepLinksDialogNavKey(deepLink = link)),
                    null
                )
            },
            onDismiss = {
                remove(key)
                onDialogHandled()
            },
        )
    }
}
