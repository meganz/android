package mega.privacy.android.app.presentation.meeting.chat.view.bottombar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.chat.SetChatDraftMessageUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat bottom bar view model
 *
 */
@HiltViewModel
class ChatBottomBarViewModel @Inject constructor(
    private val setChatDraftMessageUseCase: SetChatDraftMessageUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val chatId: Long = savedStateHandle[Constants.CHAT_ID]
        ?: throw IllegalStateException("Chat screen must have a chat room id")

    /**
     * Save draft message
     *
     * @param draftMessage
     */
    fun saveDraftMessage(draftMessage: String, editingMessageId: Long?) {
        applicationScope.launch {
            Timber.d("Save draft message: $draftMessage")
            runCatching {
                setChatDraftMessageUseCase(
                    chatId = chatId,
                    draftMessage = draftMessage,
                    editingMessageId = editingMessageId,
                )
            }.onFailure {
                Timber.e(it, "Error saving draft message")
            }
        }
    }
}