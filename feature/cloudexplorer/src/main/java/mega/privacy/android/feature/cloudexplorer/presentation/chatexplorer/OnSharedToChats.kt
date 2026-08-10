package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Returns a handler that finalizes a "share to chats" flow: when a single chat
 * is chosen it navigates to that chat, otherwise it shows the "Sent as message"
 * snackbar. In both branches [onCloseExplorerScreen] is invoked after the user-visible feedback
 * completes (after navigation, or after the snackbar has been shown).
 */
@Composable
internal fun rememberOnSharedToChats(
    onNavigate: (NavKey) -> Unit,
    onCloseExplorerScreen: () -> Unit,
): (List<Long>) -> Unit {
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()

    return { chatIds ->
        when {
            chatIds.isEmpty() -> Unit
            chatIds.size == 1 -> {
                onNavigate(ChatNavKey(chatId = chatIds[0]))
                onCloseExplorerScreen()
            }

            else -> coroutineScope.launch {
                val result = snackbarHostState?.showAutoDurationSnackbar(
                    resources.getString(sharedR.string.general_chat_sent_as_message),
                    resources.getString(sharedR.string.general_view_button),
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNavigate(ChatListNavKey())
                }
                onCloseExplorerScreen()
            }
        }
    }
}
