package mega.privacy.android.app.presentation.settings.compose.appearance

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor

@Serializable
data object MediaDiscoverySettings : NavKey

fun NavGraphBuilder.mediaDiscoverySettings() {
    composable<MediaDiscoverySettings> { backStackEntry ->
        MegaText("This is the MediaDiscovery screen", textColor = TextColor.Error)
    }
}
