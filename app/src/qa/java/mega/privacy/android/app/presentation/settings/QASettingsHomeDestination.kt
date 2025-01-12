package mega.privacy.android.app.presentation.settings

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.view.QASettingsHomeView

@Serializable
internal object QASettingsHome

internal fun NavGraphBuilder.qaSettingsHomeDestination() {
    composable<QASettingsHome> {
        QASettingsHomeView()
    }
}