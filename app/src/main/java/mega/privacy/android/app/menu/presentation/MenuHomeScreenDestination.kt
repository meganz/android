package mega.privacy.android.app.menu.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object MenuHomeScreen : NavKey

fun NavGraphBuilder.menuHomeScreen(onNavigate: (Any) -> Unit) {
    composable<MenuHomeScreen> {
        MenuHomeScreen(
            navigateToFeature = { onNavigate(it) },
        )
    }
}