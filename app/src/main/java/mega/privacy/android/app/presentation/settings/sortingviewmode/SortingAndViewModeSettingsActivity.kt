package mega.privacy.android.app.presentation.settings.sortingviewmode

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.settings.presentation.SortingAndViewModeSettingsViewModel
import mega.privacy.android.feature.settings.presentation.view.SortingAndViewModeSettingsView
import javax.inject.Inject

/**
 * Activity that hosts the Sorting and view mode settings screen.
 */
@AndroidEntryPoint
class SortingAndViewModeSettingsActivity : AppCompatActivity() {

    /**
     * monitorThemeModeUseCase
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val viewModel = hiltViewModel<SortingAndViewModeSettingsViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MegaAppContainer(
                themeMode = themeMode,
            ) {
                SortingAndViewModeSettingsView(
                    uiState = uiState,
                    onSetSortingPreference = viewModel::setSortingPreference,
                    onSetViewModePreference = viewModel::setViewModePreference,
                    onNavigateBack = { supportFinishAfterTransition() },
                )
            }
        }
    }
}
