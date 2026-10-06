package mega.privacy.android.feature.contact.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorNoteToSelfChatIsEmptyUseCase
import mega.privacy.android.feature.contact.conversation.model.NoteToSelfUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * Note-to-self view model. Resolves the note-to-self chat, creating it when it does not exist yet,
 * and tracks whether it is empty so the row can be shown as a hint.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class NoteToSelfViewModel @Inject constructor(
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase,
    private val monitorNoteToSelfChatIsEmptyUseCase: MonitorNoteToSelfChatIsEmptyUseCase,
) : ViewModel() {

    /**
     * Ui state
     */
    val uiState: StateFlow<NoteToSelfUiState> by lazy(LazyThreadSafetyMode.NONE) {
        flow { emit(getNoteToSelfChatUseCase()) }
            .filterNotNull()
            .flatMapLatest { chatRoom ->
                monitorNoteToSelfChatIsEmptyUseCase(chatRoom.chatId).map { isEmpty ->
                    NoteToSelfUiState.Data(chatId = chatRoom.chatId, isHint = isEmpty)
                }
            }
            .catch { Timber.e(it, "Failed to load the note to self chat") }
            .asUiStateFlow(viewModelScope, NoteToSelfUiState.Loading)
    }
}
