package mega.privacy.android.app.presentation.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.settings.model.SetFeatureFlagPlaceHolder
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import javax.inject.Inject

@AndroidEntryPoint
class SettingsContainerActivity : ComponentActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var setFeatureFlag: SetFeatureFlagPlaceHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = hiltViewModel<SettingContainerViewModel>()
            val themeMode by getThemeMode().collectAsStateWithLifecycle(ThemeMode.System)
            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                val state by viewModel.state.collectAsStateWithLifecycle()
                MegaScaffold { padding ->
                    Column(
                        modifier = Modifier.padding(padding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        MegaText("New Settings Activity", TextColor.Primary)
                        OutlinedMegaButton(
                            text = "Switch off compose settings feature flag",
                            onClick = {
                                lifecycleScope.launch {
                                    setFeatureFlag(
                                        AppFeatures.SettingsComposeUI.name,
                                        false
                                    )
                                }
                                this@SettingsContainerActivity.finish()
                            },
                            rounded = true
                        )
                    }
                }
            }
        }
    }
}
