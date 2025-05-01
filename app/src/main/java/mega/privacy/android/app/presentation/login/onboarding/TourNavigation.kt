package mega.privacy.android.app.presentation.login.onboarding

import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TourScreen

internal fun NavGraphBuilder.tourScreen() {
    composable<TourScreen> {
        AndroidFragment(TourFragment::class.java)
    }
}

internal fun NavController.openTourScreen(navOptions: NavOptions? = null) {
    navigate(TourScreen, navOptions)
}