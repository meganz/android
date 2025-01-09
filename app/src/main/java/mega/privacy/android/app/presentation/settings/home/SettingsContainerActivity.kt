package mega.privacy.android.app.presentation.settings.home

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.R
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
import mega.privacy.android.navigation.settings.FeatureSettings
import javax.inject.Inject

@AndroidEntryPoint
class SettingsContainerActivity : FragmentActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var setFeatureFlag: SetFeatureFlagPlaceHolder

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Inject
    lateinit var featureSettings: Set<@JvmSuppressWildcards FeatureSettings>

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
                            SettingsContainerContent()
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingsContainerContent() {
        val navHostController = rememberNavController()
        val navBackStackEntry by navHostController.currentBackStackEntryAsState()

        val title = navBackStackEntry?.let { entry ->
            featureSettings.firstNotNullOfOrNull { settings: FeatureSettings ->
                settings.getTitleForDestination(
                    entry
                )
            }
        } ?: stringResource(
            R.string.action_settings
        )

        MegaScaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                MegaTopAppBar(
                    navigationType = AppBarNavigationType.Back {
                        if (navHostController.navigateUp().not()) finishAfterTransition()
                    },
                    title = title,
                )
            },
            snackbarHost = {},
            bottomBar = {
                PrimaryFilledButton(
                    modifier = Modifier.navigationBarsPadding(),
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
            },
            content = { padding ->
                NavHost(
                    navController = navHostController,
                    startDestination = SettingsGraph,
                    modifier = Modifier.padding(padding)
                ) {
                    settingsGraph(
                        navController = navHostController,
                    )

                    featureSettings.forEach {
                        it.settingsNavGraph(
                            this,
                            navHostController,
                        )
                    }
                }
            }
        )
    }
}
