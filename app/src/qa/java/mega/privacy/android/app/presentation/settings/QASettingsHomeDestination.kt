package mega.privacy.android.app.presentation.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.view.QASettingsHomeView
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
internal object QASettingsHome : NavKey

internal fun NavGraphBuilder.qaSettingsHomeDestination(navigationHandler: NavigationHandler) {

    composable<QASettingsHome> {
        QASettingsHomeView()
    }
}