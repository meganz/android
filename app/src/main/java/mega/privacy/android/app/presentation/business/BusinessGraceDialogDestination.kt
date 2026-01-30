package mega.privacy.android.app.presentation.business

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import mega.privacy.android.app.globalmanagement.MyAccountInfo
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.destination.BusinessGraceDialogNavKey
import javax.inject.Inject

data object BusinessGraceDialogDestinations : AppDialogDestinations {
    override val navigationGraph: EntryProviderScope<DialogNavKey>.(NavigationHandler, () -> Unit) -> Unit =
        { navigationHandler, onHandled ->
            businessGraceDialogDestination(
                remove = navigationHandler::remove,
                onDialogHandled = onHandled
            )
        }
}

fun EntryProviderScope<DialogNavKey>.businessGraceDialogDestination(
    remove: (NavKey) -> Unit,
    onDialogHandled: () -> Unit,
) {
    entry<BusinessGraceDialogNavKey>(
        metadata = DialogSceneStrategy.dialog()
    ) { key ->
        BusinessGraceDialogView(
            onDismissRequest = {
                onDialogHandled()
                remove(key)
            },
        )
    }
}
