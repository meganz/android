package mega.privacy.android.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.business.BusinessExpiredAlertScreen
import mega.privacy.android.app.presentation.business.BusinessExpiredAlertViewModel
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import javax.inject.Inject

/**
 * The activity for showing the business or pro flexi expired alert
 */
@AndroidEntryPoint
class BusinessExpiredAlertActivity : FragmentActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    private val viewModel: BusinessExpiredAlertViewModel by viewModels()

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            SessionContainer {
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    PasscodeContainer(
                        content = {
                            BusinessExpiredAlertScreen(
                                uiState = uiState,
                                onDismiss = { finish() },
                            )
                        },
                    )
                }
            }
        }
    }
}
