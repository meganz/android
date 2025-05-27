package mega.privacy.android.app.presentation.login.onboarding

import androidx.activity.compose.BackHandler
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object TourScreen

internal fun NavGraphBuilder.tourScreen(
    onBackPressed: () -> Unit,
) {
    composable<TourScreen> {
        BackHandler(onBack = onBackPressed)
        AndroidFragment(TourFragment::class.java)
    }
}

internal fun NavController.openTourScreen(navOptions: NavOptions? = null) {
    navigate(TourScreen, navOptions)
}