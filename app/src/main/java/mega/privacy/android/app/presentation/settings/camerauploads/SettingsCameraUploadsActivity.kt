package mega.privacy.android.app.presentation.settings.camerauploads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.extensions.enableEdgeToEdgeAndConsumeInsets
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * An Activity that shows the Settings Camera Uploads screen
 */
@AndroidEntryPoint
class SettingsCameraUploadsActivity : ComponentActivity() {

    /**
     * Retrieves the Device Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Handles the Passcode
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdgeAndConsumeInsets()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            SessionContainer {
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = { PsaContainer { SettingsCameraUploadsScreen() } },
                    )
                }
            }
        }
    }
}