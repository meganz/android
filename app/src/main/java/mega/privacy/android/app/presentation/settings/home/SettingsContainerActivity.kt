package mega.privacy.android.app.presentation.settings.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.presentation.settings.model.SetFeatureFlagPlaceHolder
import mega.privacy.android.app.presentation.settings.navigation.SettingsGraph
import mega.privacy.android.app.presentation.settings.navigation.settingsGraph
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

@AndroidEntryPoint
class SettingsContainerActivity : ComponentActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var setFeatureFlag: SetFeatureFlagPlaceHolder

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
                            content()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun content() {
        val navHostController = rememberNavController()
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NavHost(
                navController = navHostController,
                startDestination = SettingsGraph,
                modifier = Modifier.fillMaxHeight(0.8f)
            ) {
                settingsGraph(
                    onBackPressed = { finishAfterTransition() },
                    navController = navHostController,
                )
            }
            PrimaryFilledButton(
                modifier = Modifier,
                text = "Switch off compose settings feature flag",
                onClick = {
                    lifecycleScope.launch {
                        setFeatureFlag(
                            AppFeatures.SettingsComposeUI.name, false
                        )
                    }
                    this@SettingsContainerActivity.finish()
                },
            )
        }
    }
}
