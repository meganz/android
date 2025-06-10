package mega.privacy.android.app.presentation.permissions

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.permissions.view.NotificationsPermissionView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import javax.inject.Inject

/**
 * Activity for requesting notifications permission for Android versions greater than or equal to
 * [Build.VERSION_CODES.TIRAMISU].
 * It will ask for the permission if it is not enabled after the first login, and if it has not been
 * already requested for some of this actions:
 *  - Download
 *  - Upload
 *  - Play audio
 *  - Open Contacts section
 *  - Open Chat section
 *  - Enable CU
 *
 * @property passCodeFacade                    [PasscodeCheck]
 * @property monitorThemeModeUseCase                      [MonitorThemeModeUseCase]
 * @property sharingScope                      [CoroutineScope]
 */
@AndroidEntryPoint
class NotificationsPermissionActivity : ComponentActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            onBackPressedDispatcher.onBackPressed()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            OriginalTheme(isDark = themeMode.isDarkMode()) {
                NotificationsPermissionView()
            }
        }
    }

    @Composable
    private fun NotificationsPermissionView() {
        NotificationsPermissionView(
            onNotNowClicked = { onBackPressedDispatcher.onBackPressed() },
            onGrantAccessClicked = {
                requestNotificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        )
    }
}
