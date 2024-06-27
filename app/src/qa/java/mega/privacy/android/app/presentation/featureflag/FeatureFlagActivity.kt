package mega.privacy.android.app.presentation.featureflag

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

/**
 * Dialog to show feature flag view
 */
@OptIn(ExperimentalComposeUiApi::class)
@AndroidEntryPoint
class FeatureFlagActivity : BaseActivity() {

    private val featureFlagMenuViewModel by viewModels<FeatureFlagMenuViewModel>()

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
                    val uiState by featureFlagMenuViewModel.state.collectAsStateWithLifecycle()
                    OriginalTempTheme(isDark = mode.isDarkMode()) {
                        MegaScaffold(
                            modifier = Modifier.semantics {
                                testTagsAsResourceId = true
                            },
                            topBar = {
                                MegaAppBar(
                                    appBarType = AppBarType.BACK_NAVIGATION,
                                    title = stringResource(id = R.string.settings_qa_feature_flags_title),
                                    onNavigationPressed = ::finish
                                )
                            }
                        ) { paddingValues ->
                            FeatureFlagBody(
                                featureFlags = uiState.filteredFeatureFlags,
                                onFeatureFlagChecked = featureFlagMenuViewModel::setFeatureEnabled,
                                displayDescriptions = uiState.showDescription,
                                filter = uiState.filter,
                                onFilterChanged = featureFlagMenuViewModel::onFilterChanged,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                            )
                        }
                    }
                }
            }
        )
    }

    companion object {
        /**
         * Tag for logging
         */
        const val TAG = "FeatureFlagDialogFragment"

        /**
         * Creates instance of this class
         *
         * @return FeatureFlagDialogFragment new instance
         */
        fun newInstance() = FeatureFlagActivity()
    }
}
