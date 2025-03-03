package mega.privacy.android.app.presentation.settings.compose.home

import android.os.Parcelable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.navigation.FakeSettingsView

@Serializable
@Parcelize
data object EmptySettingsView : Parcelable

fun NavGraphBuilder.emptySettings(
    navHostController: NavHostController,
) {
    composable<EmptySettingsView> { backStackEntry ->
        FakeSettingsView("Empty")
    }
}