package mega.privacy.android.app.presentation.login.onboarding.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.login.onboarding.TourFragment
import mega.privacy.android.app.presentation.login.onboarding.model.TourUiState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.IsUrlMatchesRegexUseCase
import mega.privacy.android.domain.usecase.login.SetLogoutInProgressFlagUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * The [ViewModel] class for [TourFragment].
 *
 * @property setLogoutInProgressFlagUseCase A use case class to set the logout progress status.
 */
@HiltViewModel
class TourViewModel @Inject constructor(
    private val setLogoutInProgressFlagUseCase: SetLogoutInProgressFlagUseCase,
    private val isUrlMatchesRegexUseCase: IsUrlMatchesRegexUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourUiState())
    internal val uiState = _uiState.asStateFlow()

    internal fun clearLogoutProgressFlag() {
        viewModelScope.launch {
            Timber.d("Clearing the logout progress status")
            runCatching { setLogoutInProgressFlagUseCase(false) }
                .onFailure { Timber.e("Failed to set the logout progress status", it) }
        }
    }

    internal fun onMeetingLinkChange(meetingLink: String) {
        _uiState.update {
            it.copy(
                meetingLink = meetingLink,
                errorTextId = null
            )
        }
    }

    internal fun onConfirmMeetingLinkClick() {
        if (_uiState.value.meetingLink.isBlank()) {
            _uiState.update { it.copy(errorTextId = R.string.invalid_meeting_link_empty) }
            return
        }

        checkLinkValidity()
    }

    private fun checkLinkValidity() {
        viewModelScope.launch {
            runCatching {
                Timber.d("Matching the meeting link regex")
                // Meeting Link and Chat Link are exactly the same format.
                // Using extra approach(getMegaHandleList of openChatPreview())
                // to judge if its a meeting link later on
                isUrlMatchesRegexUseCase(
                    url = _uiState.value.meetingLink,
                    patterns = Constants.CHAT_LINK_REGEXS
                )
            }.onSuccess { matches ->
                if (matches) {
                    // Need to call the async checkChatLink() to check if the chat has a call and
                    // get the meeting name
                    // Delegate the checking to OpenLinkActivity
                    // If yes, show Join Meeting, If no, show Chat history
                    _uiState.update { it.copy(shouldOpenLink = true) }
                } else {
                    _uiState.update { it.copy(errorTextId = R.string.invalid_meeting_link_args) }
                }
            }.onFailure {
                Timber.e("Failed to match the meeting link regex", it)
            }
        }
    }

    internal fun resetOpenLink() {
        _uiState.update { it.copy(shouldOpenLink = false) }
    }
}
