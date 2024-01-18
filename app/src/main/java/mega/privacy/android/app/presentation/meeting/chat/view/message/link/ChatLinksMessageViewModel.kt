package mega.privacy.android.app.presentation.meeting.chat.view.message.link

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.contact.GetContactFromLinkUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicLinkInformationUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeUseCase
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Chat link message view model
 *
 */
@HiltViewModel
class ChatLinksMessageViewModel @Inject constructor(
    private val getContactFromLinkUseCase: GetContactFromLinkUseCase,
    private val checkChatLinkUseCase: CheckChatLinkUseCase,
    private val getPublicLinkInformationUseCase: GetPublicLinkInformationUseCase,
    private val getPublicNodeUseCase: GetPublicNodeUseCase,
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

    /**
     * Load chat link info
     *
     * @param link
     * @return
     */
    suspend fun loadChatLinkInfo(link: String): LinkContent {
        return getLinkContentFromCache(link) ?: run {
            val request = try {
                checkChatLinkUseCase(link)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                null // if different CancellationException the link is invalid
            }
            ChatGroupLinkContent(
                numberOfParticipants = request?.number ?: -1,
                name = request?.text.orEmpty(),
                link = link
            ).also { content ->
                mutex.withLock {
                    Timber.d("loadChatLinkInfo set: $content, link: $link")
                    contactLinks[link] = content
                }
            }
        }.also {
            Timber.d("loadChatLinkInfo: $it, link: $link")
        }
    }

    /**
     * Load folder link info
     *
     * @param link Link
     * @return Folder link content
     */
    suspend fun loadFolderLinkInfo(link: String) = runCatching {
        getLinkContentFromCache(link)
            ?: getPublicLinkInformationUseCase(link).let { folderInfo ->
                FolderLinkContent(
                    folderInfo = folderInfo,
                    link = link
                ).also {
                    mutex.withLock {
                        contactLinks[link] = it
                    }
                }
            }
    }.onFailure {
        Timber.e(it, "Failed to get folder link info")
    }.getOrNull()

    /**
     * Load file link info
     *
     * @param link Link
     * @return File link content
     */
    suspend fun loadFileLinkInfo(link: String) = runCatching {
        getLinkContentFromCache(link)
            ?: getPublicNodeUseCase(link).let { node ->
                FileLinkContent(
                    node = node,
                    link = link
                ).also {
                    mutex.withLock {
                        contactLinks[link] = it
                    }
                }
            }
    }.onFailure {
        Timber.e(it, "Failed to get file link info")
    }.getOrNull()

    private suspend fun getLinkContentFromCache(link: String) = mutex.withLock {
        contactLinks[link]
    }
}