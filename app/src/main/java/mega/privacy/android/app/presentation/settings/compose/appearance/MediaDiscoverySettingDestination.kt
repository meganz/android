package mega.privacy.android.app.presentation.settings.compose.appearance

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.navigation.contract.navkey.NoSessionNavKey

@Serializable
data object MediaDiscoverySettings : NoSessionNavKey.Optional

fun NavGraphBuilder.mediaDiscoverySettings() {
    composable<MediaDiscoverySettings> { backStackEntry ->
        MegaText("This is the MediaDiscovery screen", textColor = TextColor.Error)
    }
}
