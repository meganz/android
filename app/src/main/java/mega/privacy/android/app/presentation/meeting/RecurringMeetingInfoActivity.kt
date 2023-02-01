package mega.privacy.android.app.presentation.meeting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.app.presentation.meeting.view.RecurringMeetingInfoView
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity which shows occurrences of recurring meeting.
 *
 * @property getThemeMode   [GetThemeMode]
 */
@AndroidEntryPoint
class RecurringMeetingInfoActivity : PasscodeActivity() {
    @Inject
    lateinit var getThemeMode: GetThemeMode
    private val viewModel by viewModels<RecurringMeetingInfoViewModel>()

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { View() }
    }

    @Composable
    private fun View() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsState()
        AndroidTheme(isDark = isDark) {
            RecurringMeetingInfoView(
                state = uiState,
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onBackPressed = { finish() },
                onOccurrenceClicked = { },
                onSeeMoreClicked = { },
            )
        }
    }
}