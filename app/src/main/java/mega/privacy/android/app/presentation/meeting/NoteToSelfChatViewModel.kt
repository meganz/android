package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.presentation.meeting.model.NoteToSelfChatUIState
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatNewLabelPreferenceUseCase
import mega.privacy.android.domain.usecase.chat.GetNoteToSelfChatUseCase
import mega.privacy.android.domain.usecase.chat.SetNoteToSelfChatNewLabelPreferenceUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.chat.MonitorNoteToSelfChatIsEmptyUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for note to self chat
 *
 * @property state [NoteToSelfChatUIState]
 * @property getFeatureFlagValueUseCase [GetFeatureFlagValueUseCase]
 * @property getNoteToSelfChatUseCase [GetNoteToSelfChatUseCase]
 * @property getNoteToSelfChatPreferenceUseCase [GetNoteToSelfChatNewLabelPreferenceUseCase]
 * @property setNoteToSelfChatPreferenceUseCase [SetNoteToSelfChatNewLabelPreferenceUseCase]
 * @property monitorNoteToSelfChatIsEmptyUseCase [MonitorNoteToSelfChatIsEmptyUseCase]
 */
@HiltViewModel
class NoteToSelfChatViewModel @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getNoteToSelfChatUseCase: GetNoteToSelfChatUseCase,
    private val getNoteToSelfChatPreferenceUseCase: GetNoteToSelfChatNewLabelPreferenceUseCase,
    private val setNoteToSelfChatPreferenceUseCase: SetNoteToSelfChatNewLabelPreferenceUseCase,
    private val monitorNoteToSelfChatIsEmptyUseCase: MonitorNoteToSelfChatIsEmptyUseCase,
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
            Timber.d("Note to self chat found: ${noteToSelfChatRoom.chatId}")
            _state.update { state ->
                state.copy(
                    noteToSelfChatRoom = noteToSelfChatRoom,
                )
            }
            state.value.noteToSelfChatId?.let {
                startMonitorNoteToSelfChatIsEmptyUseCase(noteToSelfChatId = it)
            }
        }
    }

    /**
     * Monitor if note to self chat is empty
     *
     * @param noteToSelfChatId
     */
    private fun startMonitorNoteToSelfChatIsEmptyUseCase(noteToSelfChatId: Long) =
        viewModelScope.launch {
            monitorNoteToSelfChatIsEmptyUseCase(noteToSelfChatId)
                .collectLatest { isChatEmpty ->
                    if (isChatEmpty) {
                        Timber.d("Note to self chat is empty")
                    } else {
                        Timber.d("Note to self chat is not empty")
                    }
                    _state.update { it.copy(isNoteToSelfChatEmpty = isChatEmpty) }
                    if (!isChatEmpty) {
                        setNoteToSelfPreference(0)
                        _state.update { it.copy(newFeatureLabelCounter = 0) }
                    }
                }
        }

    /**
     * Get note to yourself api feature flag
     */
    fun getNoteToSelfPreference() {
        viewModelScope.launch {
            runCatching {
                getNoteToSelfChatPreferenceUseCase()
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { result ->
                when (result) {
                    INVALID_VALUE -> {
                        setNoteToSelfPreference(MAX_VALUE)
                        _state.update { it.copy(newFeatureLabelCounter = MAX_VALUE) }
                    }

                    MIN_VALUE -> _state.update { it.copy(newFeatureLabelCounter = result) }
                    else -> {
                        var newValue = result - 1
                        setNoteToSelfPreference(newValue)
                        _state.update { it.copy(newFeatureLabelCounter = newValue) }
                    }
                }
            }
        }
    }

    /**
     * Get note to yourself api feature flag
     */
    private fun setNoteToSelfPreference(counter: Int) {
        viewModelScope.launch {
            runCatching {
                setNoteToSelfChatPreferenceUseCase(counter)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    internal companion object {
        private const val MAX_VALUE = 5
        private const val MIN_VALUE = 0
        private const val INVALID_VALUE = -1

    }
}