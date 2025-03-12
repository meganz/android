package mega.privacy.android.app.presentation.settings.compose.security.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.settings.compose.security.home.securitySettingsHome

fun NavGraphBuilder.securitySettings(navHostController: NavHostController) {
    securitySettingsHome(onNavigateToPasscodeSettings = {}, onNavigateToTwoFactorSettings = {})
}