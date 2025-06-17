package mega.privacy.android.navigation.contract

import androidx.navigation.NavGraphBuilder

interface FeatureDestination {
    val navigationGraph: NavGraphBuilder.(onBack: () -> Unit, onNavigate: (Any) -> Unit) -> Unit
}