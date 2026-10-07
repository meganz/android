package mega.privacy.android.feature.chat.managehistory.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import de.palm.composestateevents.EventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.feature.chat.managehistory.ManageChatHistoryViewModel
import mega.privacy.android.feature.chat.managehistory.model.ManageChatHistoryUiState
import mega.privacy.android.feature.chat.managehistory.view.ManageChatHistoryScreen
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Manage chat history entry, rendering the Compose manage chat history screen for the chat the
 * destination was opened with. Pops itself when the chat room cannot be resolved.
 *
 * Hosted by the app module's gated `ManageChatHistoryNavKey` destination.
 *
 * @param navigationHandler
 * @param chatId id of the chat room whose history is managed, or null when entering by email.
 * @param email email of the contact whose 1:1 chat should be managed, or null when the chat id
 * is known.
 */
@SuppressLint("ComposeViewModelInjection")
@Composable
fun ManageChatHistoryEntry(
    navigationHandler: NavigationHandler,
    chatId: Long?,
    email: String?,
) {
    val viewModel =
        hiltViewModel<ManageChatHistoryViewModel, ManageChatHistoryViewModel.Factory> { factory ->
            factory.create(ManageChatHistoryViewModel.Args(chatId = chatId, email = email))
        }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = LocalSnackBarHostState.current
    val coroutineScope = rememberCoroutineScope()
    val clearedMessage = stringResource(sharedR.string.clear_history_success)
    val clearFailedMessage = stringResource(sharedR.string.clear_history_error)

    when (val current = state) {
        ManageChatHistoryUiState.Loading -> Unit

        ManageChatHistoryUiState.Unavailable -> LaunchedEffect(Unit) {
            navigationHandler.back()
        }

        is ManageChatHistoryUiState.Data -> {
            EventEffect(
                event = current.historyClearedEvent,
                onConsumed = viewModel::onHistoryClearedEventConsumed,
            ) { succeeded ->
                coroutineScope.launch {
                    snackbarHostState?.showAutoDurationSnackbar(
                        message = if (succeeded) clearedMessage else clearFailedMessage,
                    )
                }
            }
            ManageChatHistoryScreen(
                isMeeting = current.isMeeting,
                retention = current.retention,
                onBackClick = navigationHandler::back,
                onSetRetention = { retention ->
                    viewModel.onSetRetention(current.chatId, retention)
                },
                onClearHistory = { viewModel.onClearHistory(current.chatId) },
            )
        }
    }
}
