package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.usecase.contact.GetContactFromLinkUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Chat link message view model
 *
 */
@HiltViewModel
class ChatLinkMessageViewModel @Inject constructor(
    private val getContactFromLinkUseCase: GetContactFromLinkUseCase,
) : ViewModel() {
    // check link is expensive operation, so we cache it
    private val contactLinks = mutableMapOf<String, LinkContent>()
    private val mutex = Mutex()

    /**
     * Load contact info
     *
     * @param link Link
     * @return Contact link
     */
    suspend fun loadContactInfo(link: String): LinkContent? {
        return runCatching {
            getLinkContentFromCache(link) ?: getContactFromLinkUseCase(link)?.let { contactLink ->
                ContactLinkContent(
                    content = contactLink,
                    link = link
                ).also {
                    mutex.withLock {
                        contactLinks[link] = it
                    }
                }
            }
        }.onFailure {
            Timber.e(it, "Failed to get contact from email")
        }.getOrNull()
    }

    private suspend fun getLinkContentFromCache(link: String) = mutex.withLock {
        contactLinks[link]
    }
}