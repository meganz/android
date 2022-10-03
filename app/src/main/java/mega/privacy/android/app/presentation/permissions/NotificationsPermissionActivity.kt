package mega.privacy.android.app.presentation.permissions

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.permissions.view.NotificationsPermissionView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.presentation.theme.AndroidTheme
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
 * @property getThemeMode                      [GetThemeMode]
 * @property sharingScope                      [CoroutineScope]
 */
@AndroidEntryPoint
class NotificationsPermissionActivity : ComponentActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @ApplicationScope
    @Inject
    lateinit var sharingScope: CoroutineScope

    private val requestNotificationsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            onBackPressedDispatcher.onBackPressed()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
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
