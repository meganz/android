package mega.privacy.android.app.presentation.chat.archived

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.chat.archived.view.ArchivedChatsView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Archived chats activity
 */
@AndroidEntryPoint
class ArchivedChatsActivity : AppCompatActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var navigator: MegaNavigator

    private val viewModel: ArchivedChatsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val mode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val state by viewModel.getState().collectAsStateWithLifecycle()
            AndroidTheme(isDark = mode.isDarkMode()) {
                ArchivedChatsView(
                    state = state,
                    onItemClick = ::onItemClick,
                    onItemUnarchived = viewModel::unarchiveChat,
                    onBackPressed = { finish() },
                    onSnackBarDismiss = viewModel::dismissSnackBar,
                )
            }
        }
    }

    private fun onItemClick(chatId: Long) {
        navigator.openChat(
            context = this,
            chatId = chatId,
            action = Constants.ACTION_CHAT_SHOW_MESSAGES,
        )
    }
}
