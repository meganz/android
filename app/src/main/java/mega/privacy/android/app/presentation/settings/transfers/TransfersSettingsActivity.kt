package mega.privacy.android.app.presentation.settings.transfers

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.feature.transfers.presentation.settings.TransfersSettingsViewModel
import mega.privacy.android.feature.transfers.presentation.settings.view.TransfersSettingsView
import javax.inject.Inject

/**
 * Activity which allows to change the transfers settings.
 */
@AndroidEntryPoint
class TransfersSettingsActivity : AppCompatActivity() {

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
            val viewModel = hiltViewModel<TransfersSettingsViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MegaAppContainer(
                themeMode = themeMode,
            ) {
                TransfersSettingsView(
                    uiState = uiState,
                    onSetMaxDownloadConnections = viewModel::setMaxDownloadConnections,
                    onSetMaxUploadConnections = viewModel::setMaxUploadConnections,
                    onNavigateBack = { supportFinishAfterTransition() },
                )
            }
        }
    }
}
