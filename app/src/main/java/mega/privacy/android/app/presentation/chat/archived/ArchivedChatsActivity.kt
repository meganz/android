package mega.privacy.android.app.presentation.chat.archived

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.chat.archived.view.ArchivedChatsView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.meeting.NoteToSelfChatViewModel
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * Archived chats activity
 */
@AndroidEntryPoint
class ArchivedChatsActivity : AppCompatActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var navigator: MegaNavigator

    private val viewModel: ArchivedChatsViewModel by viewModels()
    private val noteToSelfChatViewModel by viewModels<NoteToSelfChatViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        collectFlow(noteToSelfChatViewModel.state.map { it.noteToSelfChatRoom }
            .distinctUntilChanged()) {
            it?.also {
                noteToSelfChatViewModel.getNoteToSelfPreference()
            }
        }

        setContent {
            val mode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val state by viewModel.getState().collectAsStateWithLifecycle()
            val noteToSelfState by noteToSelfChatViewModel.state.collectAsStateWithLifecycle()

            OriginalTheme(isDark = mode.isDarkMode()) {
                ArchivedChatsView(
                    state = state,
                    noteToSelfState = noteToSelfState,
                    onItemClick = ::onItemClick,
                    onItemUnarchived = viewModel::unarchiveChat,
                    onBackPressed = { finish() },
                    onSnackBarDismiss = viewModel::dismissSnackBar,
                )
            }
        }
    }

    private fun onItemClick(chatId: Long, isNoteToSelfChat: Boolean) {
        Timber.d("ArchivedChatsActivity onItemClick: chatId=$chatId, isNoteToSelfChat=$isNoteToSelfChat")
        navigator.openChat(
            context = this,
            chatId = chatId,
            action = Constants.ACTION_CHAT_SHOW_MESSAGES,
        )
    }
}
