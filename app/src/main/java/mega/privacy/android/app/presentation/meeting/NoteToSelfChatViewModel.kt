package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.IsAnEmptyChatUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for note to self chat
 *
 * @property state [NoteToSelfChatUIState]
 */
@HiltViewModel
class NoteToSelfChatViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase,
    private val isAnEmptyChatUseCase: IsAnEmptyChatUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(NoteToSelfChatUIState())
    val state = _state.asStateFlow()

    init {
        getApiFeatureFlag()
    }

    /**
     * Get note to yourself api feature flag
     */
    private fun getApiFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.NoteToYourselfFlag).also {
                    _state.update { state ->
                        state.copy(
                            isNoteToYourselfFeatureFlagEnabled = it,
                        )
                    }
                    if (it) {
                        getNoteToSelfChat()
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Get note to self chat
     */
    private suspend fun getNoteToSelfChat() {
        getNoteToSelfChatUseCase()?.let { noteToSelfChatRoom ->
            _state.update { state ->
                state.copy(
                    noteToSelfChatRoom = noteToSelfChatRoom,
                )
            }
            isAnEmptyChatUseCase(noteToSelfChatRoom.chatId).let { isChatEmpty ->
                Timber.d("Check if note to self chat is empty: $isChatEmpty")
                _state.update { it.copy(isNoteToSelfChatEmpty = isChatEmpty) }
            }
        }
    }
}