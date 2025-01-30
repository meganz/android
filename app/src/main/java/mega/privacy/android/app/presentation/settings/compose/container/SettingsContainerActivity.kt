package mega.privacy.android.app.presentation.settings.compose.container

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.compose.navigation.SettingsGraph
import mega.privacy.android.app.presentation.settings.compose.navigation.settingsGraph
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class SettingsContainerActivity : FragmentActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SessionContainer {
                val themeMode by getThemeMode().collectAsStateWithLifecycle(ThemeMode.System)
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(
                        passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                        content = {
                            PsaContainer {
                                val navHostController = rememberNavController()
                                NavHost(
                                    navController = navHostController,
                                    startDestination = SettingsGraph,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    settingsGraph(
                                        navController = navHostController,
                                        {
                                            if (navHostController.popBackStack()
                                                    .not()
                                            ) finishAfterTransition()
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
