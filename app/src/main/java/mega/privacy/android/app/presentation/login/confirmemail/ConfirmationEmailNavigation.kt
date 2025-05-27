package mega.privacy.android.app.presentation.login.confirmemail

import androidx.activity.compose.BackHandler
import androidx.fragment.compose.AndroidFragment
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data object ConfirmationEmailScreen

internal fun NavGraphBuilder.confirmationEmailScreen(
    onBackPressed: () -> Unit,
) {
    composable<ConfirmationEmailScreen> {
        BackHandler(onBack = onBackPressed)
        AndroidFragment(ConfirmEmailFragment::class.java)
    }
}

internal fun NavController.openConfirmationEmailScreen(navOptions: NavOptions? = null) {
    navigate(ConfirmationEmailScreen, navOptions)
}