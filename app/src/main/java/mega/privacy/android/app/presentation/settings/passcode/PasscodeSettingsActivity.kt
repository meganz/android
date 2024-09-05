package mega.privacy.android.app.presentation.settings.passcode

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.passcode.biometric.BiometricAuth
import mega.privacy.android.app.presentation.settings.passcode.navigation.PasscodeSettingsDestination
import mega.privacy.android.app.presentation.settings.passcode.navigation.PasscodeTimeOutDestination
import mega.privacy.android.app.presentation.settings.passcode.navigation.passCodeSettings
import mega.privacy.android.app.presentation.settings.passcode.navigation.passCodeTimeOut
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * An Activity that shows the Settings Camera Uploads screen
 */
@AndroidEntryPoint
class PasscodeSettingsActivity() : FragmentActivity() {

    /**
     * Retrieves the Device Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Handles the Passcode
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var biometricAuth: BiometricAuth

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            SessionContainer {
                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = {
                            PasscodeSettingsGraph(biometricAuth)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PasscodeSettingsGraph(biometricAuth: BiometricAuth) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = PasscodeSettingsDestination) {
        passCodeSettings(
            navController = navController,
            navigateToSelectTimeout = { navController.navigate(PasscodeTimeOutDestination) },
            biometricAuth = biometricAuth
        )

        passCodeTimeOut(
            navController = navController
        )
    }
}