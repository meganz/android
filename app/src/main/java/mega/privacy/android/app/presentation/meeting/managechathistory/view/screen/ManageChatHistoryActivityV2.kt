package mega.privacy.android.app.presentation.meeting.managechathistory.view.screen

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.theme.MegaAppTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * This activity displays a screen allowing the user to manage the chat history. Options:
 * - The user can clear the chat history
 * - The user can set the retention time for the chat history
 *
 * This activity will replace the [mega.privacy.android.app.activities.ManageChatHistoryActivity]
 */
@AndroidEntryPoint
class ManageChatHistoryActivityV2 : PasscodeActivity() {

    /**
     * Current theme
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * Called when the activity is starting.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null || intent.extras == null) {
            Timber.e("Cannot init view, Intent is null")
            finish()
        }

        setContent {
            val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                ManageChatHistoryRoute(
                    modifier = Modifier.fillMaxSize(),
                    onRetryConnectionsAndSignalPresence = ::retryConnectionsAndSignalPresence,
                    onNavigateUp = ::finish
                )
            }
        }
    }
}
