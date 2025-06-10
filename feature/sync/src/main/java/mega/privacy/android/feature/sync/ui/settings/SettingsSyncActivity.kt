package mega.privacy.android.feature.sync.ui.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.sync.ui.isDarkMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Activity to show the Sync Settings
 */
@AndroidEntryPoint
class SettingsSyncActivity : ComponentActivity() {

    /**
     * Retrieves the Device Theme Mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            OriginalTheme(isDark = themeMode.isDarkMode()) {
                SettingsSyncRoute()
            }
        }
    }
}

