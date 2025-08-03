package mega.privacy.android.app.presentation.settings.compose.security.navigation

import androidx.navigation.NavGraphBuilder
import mega.privacy.android.app.presentation.settings.compose.security.home.securitySettingsHome
import mega.privacy.android.navigation.contract.NavigationHandler

fun NavGraphBuilder.securitySettings(navigationHandler: NavigationHandler) {
    securitySettingsHome(onNavigateToPasscodeSettings = {}, onNavigateToTwoFactorSettings = {})
}