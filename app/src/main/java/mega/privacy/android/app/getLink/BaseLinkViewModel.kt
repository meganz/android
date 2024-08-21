package mega.privacy.android.app.getLink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        key: String?,
        password: String?,
    ) {
        link ?: return
        viewModelScope.launch {
            runCatching {
                val createdChatIds = data.userHandles.toList().mapNotNull { userHandle ->
                    runCatching {
                        get1On1ChatIdUseCase(userHandle)
                    }.getOrNull()
                }
                val finalChatIds = createdChatIds + data.chatIds.toList()
                finalChatIds.forEach {
                    sendTextMessageUseCase(it, link)
                    if (!key.isNullOrEmpty() || !password.isNullOrEmpty()) {
                        if (!password.isNullOrEmpty()) {
                            sendTextMessageUseCase(it, password)
                        } else {
                            sendTextMessageUseCase(it, key.orEmpty())
                        }
                    }
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

    /**
     * On share link result handled.
     */
    fun onShareLinkResultHandled() {
        _sendLinkToChatResult.update { null }
    }
}