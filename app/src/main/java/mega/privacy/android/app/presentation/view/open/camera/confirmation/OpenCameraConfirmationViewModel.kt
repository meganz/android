package mega.privacy.android.app.presentation.view.open.camera.confirmation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableVideoUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallInProgress
import timber.log.Timber
import javax.inject.Inject

/**
 * A view model class for [OpenCameraConfirmationDialog]]
 */
@HiltViewModel
class OpenCameraConfirmationViewModel @Inject constructor(
    private val getChatCallInProgress: GetChatCallInProgress,
    private val enableOrDisableVideoUseCase: EnableOrDisableVideoUseCase,
) : ViewModel() {

    internal var hasSuccessfullyDisableOngoingVideo: Boolean by mutableStateOf(false)
        private set

    internal fun disableOngoingVideo() {
        viewModelScope.launch {
            Timber.d("Getting ongoing calls")
            runCatching { getChatCallInProgress() }
                .onSuccess { it?.let { disableVideo(it.chatId) } }
                .onFailure { Timber.e("Failed to get the ongoing calls") }
        }
    }

    private fun disableVideo(chatId: Long) {
        viewModelScope.launch {
            Timber.d("Disabling ongoing video")
            runCatching { enableOrDisableVideoUseCase(chatId, false) }
                .onSuccess { hasSuccessfullyDisableOngoingVideo = true }
                .onFailure { Timber.e("Failed to disable the ongoing video") }
        }
    }

    internal fun resetOngoingVideoDisablementState() {
        hasSuccessfullyDisableOngoingVideo = false
    }
}
