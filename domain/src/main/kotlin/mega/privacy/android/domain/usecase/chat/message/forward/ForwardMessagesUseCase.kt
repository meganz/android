package mega.privacy.android.domain.usecase.chat.message.forward

import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.exception.chat.CreateChatException
import mega.privacy.android.domain.exception.chat.ForwardException
import mega.privacy.android.domain.usecase.chat.CreateChatRoomUseCase
import javax.inject.Inject

/**
 * Use case to forward messages.
 */
class ForwardMessagesUseCase @Inject constructor(
    private val forwardMessageUseCases: Set<@JvmSuppressWildcards ForwardMessageUseCase>,
    private val createChatRoomUseCase: CreateChatRoomUseCase,
) {

    /**
     * Invoke.
     *
     * @param messages List of messages to forward.
     * @param chatHandles List of chat handles to forward the messages.
     * @param contactHandles List of contact handles to forward the messages.
     * @return List of results of the forward operation.
     */
    suspend operator fun invoke(
        messages: List<TypedMessage>,
        chatHandles: List<Long>?,
        contactHandles: List<Long>?,
    ): List<ForwardResult> {
        if (messages.isEmpty()) {
            throw ForwardException("No messages provided to forward.")
        }
        if (chatHandles?.isEmpty() == true && contactHandles?.isEmpty() == true) {
            throw ForwardException("No chat or contact handles provided to forward the messages.")
        }
        val finalChatHandles = buildList {
            contactHandles?.forEach { contactHandle ->
                runCatching { createChatRoomUseCase(false, listOf(contactHandle)) }
                    .getOrNull()?.let { chatHandle -> add(chatHandle) }
            }
            chatHandles?.let { addAll(it) }
        }
        val contactsSize = contactHandles?.size ?: 0

        if (chatHandles?.isEmpty() == true && contactsSize > 0 && finalChatHandles.isEmpty()) {
            throw CreateChatException()
        }

        return buildList {
            messages.sortedBy { it.time }.forEach { message ->
                val results = forwardMessageUseCases
                    .fold(emptyList<ForwardResult>()) { acc, forwardMessageUseCase ->
                        if (acc.isNotEmpty()) return@fold acc
                        forwardMessageUseCase(finalChatHandles, message)
                    }
                addAll(results)
            }
        }
    }
}