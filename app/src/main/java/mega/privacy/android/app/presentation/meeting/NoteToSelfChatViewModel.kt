package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorNoteToSelfChatIsEmptyUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for note to self chat
 *
 * @property state [NoteToSelfChatUIState]
 * @property getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 * @property getNoteToSelfChatUseCase [GetNoteToSelfChatUseCase]
 * @property monitorNoteToSelfChatIsEmptyUseCase [MonitorNoteToSelfChatIsEmptyUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NoteToSelfChatViewModel @Inject constructor(
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase,
    private val monitorNoteToSelfChatIsEmptyUseCase: MonitorNoteToSelfChatIsEmptyUseCase,
) : ViewModel() {

    val state: StateFlow<NoteToSelfChatUIState> by lazy {
        flow {
            emit(getNoteToSelfChatUseCase())
        }.filterNotNull()
            .onEach {
                Timber.d("Note to self chat found: ${it.chatId}")
            }
            .flatMapLatest { chatRoom ->
                monitorNoteToSelfChatIsEmptyUseCase(chatRoom.chatId)
                    .onEach {
                        if (it) {
                            Timber.d("Note to self chat is empty")
                        } else {
                            Timber.d("Note to self chat is not empty")
                        }
                    }
                    .onStart { emit(true) }
                    .mapLatest {
                        NoteToSelfChatUIState(
                            noteToSelfChatRoom = chatRoom,
                            isNoteToSelfChatEmpty = it,
                        )
                    }
            }.catch { Timber.e(it) }
            .asUiStateFlow(
                viewModelScope,
                NoteToSelfChatUIState(),
            )
    }
}