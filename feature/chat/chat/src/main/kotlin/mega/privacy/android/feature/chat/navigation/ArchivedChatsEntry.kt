package mega.privacy.android.feature.chat.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.feature.chat.list.archived.ArchivedChatsViewModel
import mega.privacy.android.feature.chat.list.archived.view.ArchivedChatsScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.destination.ShowChatMessagesNavKey

/**
 * Archived chats entry rendering the Compose [ArchivedChatsScreen].
 *
 * @param navigationHandler
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
internal fun ArchivedChatsEntry(
    navigationHandler: NavigationHandler,
) {
    val viewModel = hiltViewModel<ArchivedChatsViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ArchivedChatsScreen(
        uiState = uiState,
        onBackClick = navigationHandler::back,
        onItemClick = { chatId ->
            navigationHandler.navigate(ShowChatMessagesNavKey(chatId))
        },
    )
}

/**
 * Registers the [ArchivedChatsNavKey] entry that renders [ArchivedChatsEntry] within a
 * navigation graph.
 *
 * @param navigationHandler
 */
fun EntryProviderScope<NavKey>.archivedChatsScreen(
    navigationHandler: NavigationHandler,
) {
    entry<ArchivedChatsNavKey> {
        ArchivedChatsEntry(navigationHandler = navigationHandler)
    }
}
