package mega.privacy.android.app.presentation.featureflag

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.controls.appbar.AppBarType
import mega.privacy.android.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import javax.inject.Inject

/**
 * This is a QA only activity. When long click on Quick Settings tile, this activity will be displayed to allow configure which feature flag will be used in the tile
 */
@AndroidEntryPoint
class FeatureFlagForQuickSettingsTileActivity : BaseActivity() {


    private val featureFlagForQuickSettingsTileViewModel by viewModels<FeatureFlagForQuickSettingsTileViewModel>()

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * On create
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val mode by getThemeMode()
                        .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                    val uiState by featureFlagForQuickSettingsTileViewModel.state.collectAsStateWithLifecycle()
                    MegaAppTheme(isDark = mode.isDarkMode()) {
                        MegaScaffold(
                            topBar = {
                                MegaAppBar(
                                    appBarType = AppBarType.BACK_NAVIGATION,
                                    title = stringResource(id = R.string.settings_qa_feature_flag_quick_settings_title),
                                    onNavigationPressed = ::finish
                                )
                            }
                        ) { paddingValues ->
                            FeatureFlagBody(
                                featureFlags = uiState.filteredFeatureFlags,
                                onFeatureFlagChecked = { name, _ ->
                                    featureFlagForQuickSettingsTileViewModel.setFeatureEnabled(name)
                                },
                                displayDescriptions = uiState.showDescription,
                                filter = uiState.filter,
                                onFilterChanged = featureFlagForQuickSettingsTileViewModel::onFilterChanged,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                useRadioButton = true
                            )
                        }
                    }
                }
            })
    }

}