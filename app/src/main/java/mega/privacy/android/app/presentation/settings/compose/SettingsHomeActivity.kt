package mega.privacy.android.app.presentation.settings.compose

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.compose.home.SettingsHomeDestinationWrapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class SettingsHomeActivity : FragmentActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
                {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = it
                    )
                },
                { AndroidTheme(isDark = themeMode.isDarkMode(), content = it) },
                { SessionContainer(content = it) },
            )

            AppContainer(
                containers = containers,
            ) {
                val navController = rememberNavController()

                BackHandler {
                    if (navController.popBackStack().not()) finishAfterTransition()
                }

                SettingsHomeDestinationWrapper(
                    onBackPressed = ::finishAfterTransition
                )
            }
        }
    }
}