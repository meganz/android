package mega.privacy.android.app.presentation.meeting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.presentation.extensions.changeStatusBarColor
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.app.presentation.meeting.view.RecurringMeetingInfoView
import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaChatApiJava
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

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect { (finish) ->
                    if (finish) {
                        Timber.d("Finish activity")
                        finish()
                    }
                }
            }
        }

        viewModel.setChatId(
            newChatId = intent.getLongExtra(
                Constants.CHAT_ID,
                MegaChatApiJava.MEGACHAT_INVALID_HANDLE
            )
        )

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
                onSeeMoreClicked = { viewModel.onSeeMoreOccurrencesTap() },
            )
        }
    }
}