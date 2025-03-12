package mega.privacy.android.app.presentation.settings.compose.security.home

import android.os.Parcelable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.settings.compose.security.home.view.SecuritySettingsHomeView

@Serializable
@Parcelize
data object SecuritySettings : Parcelable

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