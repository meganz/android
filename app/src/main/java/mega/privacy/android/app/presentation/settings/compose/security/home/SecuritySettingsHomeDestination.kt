package mega.privacy.android.app.presentation.settings.compose.security.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.security.home.view.SecuritySettingsHomeView

@Serializable
data object SecuritySettings : NavKey

internal fun NavGraphBuilder.securitySettingsHome(
    onNavigateToPasscodeSettings: () -> Unit,
    onNavigateToTwoFactorSettings: () -> Unit,
) {
    composable<SecuritySettings> {
        SecuritySettingsHomeView(
            onNavigateToPasscodeSettings = onNavigateToPasscodeSettings,
            onNavigateToTwoFactorSettings = onNavigateToTwoFactorSettings,
        )
    }
}