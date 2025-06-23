package mega.privacy.android.navigation.contract

import androidx.navigation.NavGraphBuilder

interface FeatureDestination {
    val navigationGraph: NavGraphBuilder.(navigationHandler: NavigationHandler) -> Unit
}