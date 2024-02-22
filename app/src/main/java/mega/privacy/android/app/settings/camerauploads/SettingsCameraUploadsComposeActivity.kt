package mega.privacy.android.app.settings.camerauploads

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import javax.inject.Inject

/**
 * An Activity that shows the Settings Camera Uploads screen in Jetpack Compose
 *
 * This Activity is used when the [mega.privacy.android.app.featuretoggle.AppFeatures.SettingsCameraUploadsCompose]
 * Feature Flag is enabled. Otherwise, the legacy
 * [mega.privacy.android.app.activities.settingsActivities.LegacyCameraUploadsPreferencesActivity] is used
 */
@AndroidEntryPoint
class SettingsCameraUploadsComposeActivity : ComponentActivity() {

    /**
     * Retrieves the Device Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                SettingsCameraUploadsScreen()
            }
        }
    }
}