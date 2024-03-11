package mega.privacy.android.app.presentation.meeting.chat.view.message.error

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.usecase.chat.message.ResendMessageUseCase
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class SendErrorViewModel @Inject constructor(
    private val resendMessageUseCase: ResendMessageUseCase,
) : ViewModel() {

    fun retry(messages: Set<TypedMessage>) {
        viewModelScope.launch {
            messages.forEach { message ->
                runCatching { resendMessageUseCase(message) }
                    .onFailure {
                        Timber.e(it, "Retry message sending failed")
                    }
            }
        }
    }
}