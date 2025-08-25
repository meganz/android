package mega.privacy.android.navigation.contract

import androidx.navigation.NavGraphBuilder

interface AppDialogDestinations {
    val navigationGraph: NavGraphBuilder.(navigationHandler: NavigationHandler, onHandled: () -> Unit) -> Unit
}