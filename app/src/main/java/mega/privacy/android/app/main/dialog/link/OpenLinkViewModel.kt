package mega.privacy.android.app.main.dialog.link

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.main.dialog.link.OpenLinkDialogFragment.Companion.IS_CHAT_SCREEN
import mega.privacy.android.app.main.dialog.link.OpenLinkDialogFragment.Companion.IS_JOIN_MEETING
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.usecase.GetUrlRegexPatternTypeUseCase
import mega.privacy.android.domain.usecase.chat.GetChatLinkContentUseCase
import mega.privacy.android.domain.usecase.chat.GetHandleFromContactLinkUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class OpenLinkViewModel @Inject constructor(
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val getHandleFromContactLinkUseCase: GetHandleFromContactLinkUseCase,
    private val getChatLinkContentUseCase: GetChatLinkContentUseCase,
) : ViewModel() {
    private val isChatScreen = savedStateHandle.get<Boolean>(IS_CHAT_SCREEN) ?: false
    private val isJoinMeeting = savedStateHandle.get<Boolean>(IS_JOIN_MEETING) ?: false

    private val _state = MutableStateFlow(OpenLinkUiState())
    val state = _state.asStateFlow()

    fun onLinkChanged(link: String) {
        savedStateHandle[CURRENT_INPUT_LINK] = link
        _state.update { state ->
            state.copy(
                linkType = null,
                checkLinkResult = null,
                submittedLink = null
            )
        }
    }

    val inputLink: String
        get() = savedStateHandle.get<String>(CURRENT_INPUT_LINK).orEmpty()

    fun openLink(link: String) {
        _state.update { state -> state.copy(submittedLink = link) }
        if (link.isNotEmpty()) {
            val linkType = _state.value.linkType
            if (linkType == RegexPatternType.CONTACT_LINK) {
                openContactLink(link)
            } else if ((isChatScreen || isJoinMeeting) || linkType == RegexPatternType.CHAT_LINK) {
                openChatOrMeetingLink(link)
            } else {
                getLinkType(link)
            }
        }
    }

    private fun openChatOrMeetingLink(link: String) {
        viewModelScope.launch {
            val result = runCatching { getChatLinkContentUseCase(link) }
                .onFailure {
                    Timber.e(it)
                }
            _state.update { state -> state.copy(checkLinkResult = result) }
        }
    }

    fun openContactLink(link: String) {
        viewModelScope.launch {
            runCatching {
                getHandleFromContactLinkUseCase(link)
            }.onSuccess { handle ->
                _state.update { state -> state.copy(openContactLinkHandle = handle) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getLinkType(link: String) {
        val linkType = getUrlRegexPatternTypeUseCase(link)
        _state.update { state -> state.copy(linkType = linkType) }
    }

    companion object {
        // handle case process recreate we need to save to SavedStateHandle
        const val CURRENT_INPUT_LINK = "CURRENT_INPUT_LINK"
    }
}