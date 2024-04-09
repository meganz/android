package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.usecase.chat.GetLinksFromMessageContentUseCase
import javax.inject.Inject

/**
 * Message content view model.
 */
@HiltViewModel
class MessageContentViewModel @Inject constructor(
    private val getLinksFromMessageContentUseCase: GetLinksFromMessageContentUseCase,
) : ViewModel() {

    /**
     * Get links from message content
     *
     * @param content Message content.
     * @return List of links.
     */
    fun getLinks(content: String) = getLinksFromMessageContentUseCase(content)
}