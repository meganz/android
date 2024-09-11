package mega.privacy.android.app.getLink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import mega.privacy.android.app.main.model.SendToChatResult
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import timber.log.Timber

/**
 * Base ViewModel to share a link to chat.
 */
abstract class BaseLinkViewModel(
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
) : ViewModel() {
    private val _sendLinkToChatResult = MutableStateFlow<SendLinkResult?>(null)

    /**
     * Send link to chat result.
     */
    val sendLinkToChatResult = _sendLinkToChatResult.asStateFlow()

    /**
     * Shares the link and extra content if enabled (decryption key or password) to chat.
     *
     * @param data                      Intent containing the info to share the content to chats.
     * @param link                      The link to share.
     * @param key                       The decryption key to share.
     * @param password                  The password to share.
     */
    fun sendToChat(
        data: SendToChatResult,
        link: String?,
        key: String? = null,
        password: String? = null,
    ) {
        link?.let {
            sendToChat(data, listOf(it), key, password)
        }
    }

    /**
     * Shares the link to chat.
     */
    fun sendToChat(
        data: SendToChatResult,
        links: List<String>,
    ) {
        sendToChat(data, links, null, null)
    }

    private fun sendToChat(
        data: SendToChatResult,
        links: List<String>,
        key: String?,
        password: String?,
    ) {
        viewModelScope.launch {
            val semaphore = Semaphore(10)
            runCatching {
                val createdChatIds = createNewChat(data.userHandles.toList())
                val finalChatIds = createdChatIds + data.chatIds.toList()
                finalChatIds.map {
                    links.map { link ->
                        async {
                            semaphore.withPermit {
                                runCatching {
                                    sendTextMessageUseCase(it, link)
                                }.onFailure {
                                    Timber.e(it)
                                }
                            }
                        }
                    }
                }.flatten().awaitAll()
                if (key.isNullOrEmpty() || password.isNullOrEmpty()) {
                    finalChatIds.map {
                        async {
                            semaphore.withPermit {
                                runCatching {
                                    if (!password.isNullOrEmpty()) {
                                        sendTextMessageUseCase(it, password)
                                    } else {
                                        sendTextMessageUseCase(it, key.orEmpty())
                                    }
                                }.onFailure {
                                    Timber.e(it)
                                }
                            }
                        }
                    }.awaitAll()
                }
                if (finalChatIds.size == 1) finalChatIds.first() else -1L
            }.onSuccess { openChatId ->
                if (!password.isNullOrEmpty()) {
                    _sendLinkToChatResult.update { SendLinkResult.LinkWithPassword(openChatId) }
                } else if (!key.isNullOrEmpty()) {
                    _sendLinkToChatResult.update { SendLinkResult.LinkWithKey(openChatId) }
                } else {
                    _sendLinkToChatResult.update { SendLinkResult.NormalLink(openChatId) }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private suspend fun createNewChat(userHandles: List<Long>) =
        userHandles.mapNotNull { userHandle ->
            runCatching {
                get1On1ChatIdUseCase(userHandle)
            }.getOrNull()
        }

    /**
     * On share link result handled.
     */
    fun onShareLinkResultHandled() {
        _sendLinkToChatResult.update { null }
    }
}