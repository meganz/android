package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import android.os.Build
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
import mega.privacy.android.app.presentation.meeting.view.CustomRecurrenceView
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.chat.ChatScheduledRules
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Activity which shows custom recurrence screen.
 *
 * @property passCodeFacade [PasscodeCheck]
 * @property getThemeMode   [GetThemeMode]
 */
@AndroidEntryPoint
class CustomRecurrenceActivity : PasscodeActivity() {

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<CustomRecurrenceViewModel>()

    companion object {
        const val RECURRING_RULES = "RECURRING_RULES"
    }

    /**
     * Perform Activity initialization
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rules = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                RECURRING_RULES,
                ChatScheduledRules::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(RECURRING_RULES)
        } as ChatScheduledRules?

        viewModel.setRules(rules)

        setContent { CustomRecurrenceActivityComposeView() }
    }


    @Composable
    private fun CustomRecurrenceActivityComposeView() {
        val themeMode by getThemeMode().collectAsState(initial = ThemeMode.System)
        val isDark = themeMode.isDarkMode()
        val uiState by viewModel.state.collectAsState()
        AndroidTheme(isDark = isDark) {
            CustomRecurrenceView(
                state = uiState,
                onAcceptClicked = {
                    setResult(
                        RESULT_OK, Intent()
                            .putExtra(RECURRING_RULES, viewModel.getRules())
                    )
                    finish()
                },
                onScrollChange = { scrolled -> this.changeStatusBarColor(scrolled, isDark) },
                onBackPressed = {
                    finish()
                },
                onTypeClicked = { viewModel.onSelectType(it) },
                onNumberClicked = { viewModel.onSelectNumber(it) },
                onWeekdaysClicked = { viewModel.onWeekdaysOptionClicked() },
                onFocusChanged = { viewModel.onFocusChanged() }
            )
        }
    }
}