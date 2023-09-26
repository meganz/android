package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isVoiceClip
import mega.privacy.android.domain.entity.transfer.pendingMessageId
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.transfers.GetTransferByTagUseCase
import mega.privacy.android.domain.usecase.transfers.GetTransferDataUseCase
import javax.inject.Inject
import kotlin.coroutines.coroutineContext

/**
 * Load pending messages use case
 *
 * @property chatRepository [ChatRepository]
 * @property getTransferByTagUseCase [GetTransferByTagUseCase]
 * @property getTransferDataUseCase [GetTransferDataUseCase].
 * @constructor Create empty Load pending messages use case
 */
class LoadPendingMessagesUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
    private val getTransferByTagUseCase: GetTransferByTagUseCase,
    private val getTransferDataUseCase: GetTransferDataUseCase,
) {

    /**
     * Invoke
     *
     * @param chatId
     */
    suspend operator fun invoke(chatId: Long) = flow {
        val invalidOption = "-1"
        val invalidTransferTag = -1

        chatRepository.getPendingMessages(chatId).forEach { pendingMessage ->
            coroutineContext.ensureActive()
            with(pendingMessage) {
                when {
                    transferTag != invalidTransferTag -> {
                        val transfer = getTransferByTagUseCase(transferTag)
                            ?: findTransferFromPendingMessage(id)

                        when {
                            transfer == null || transfer.state == TransferState.STATE_FAILED -> {
                                val tag = transfer?.tag ?: invalidTransferTag
                                chatRepository.updatePendingMessage(
                                    id,
                                    tag,
                                    invalidOption,
                                    PendingMessageState.ERROR_UPLOADING.value
                                )
                                emit(pendingMessage.copy(state = PendingMessageState.ERROR_UPLOADING.value))
                            }

                            transfer.state == TransferState.STATE_COMPLETED || transfer.state == TransferState.STATE_CANCELLED -> {
                                chatRepository.updatePendingMessage(
                                    id,
                                    transfer.tag,
                                    invalidOption,
                                    PendingMessageState.SENT.value
                                )
                            }

                            else -> {
                                emit(pendingMessage)
                            }
                        }
                    }

                    state == PendingMessageState.PREPARING_FROM_EXPLORER.value -> {
                        chatRepository.updatePendingMessage(
                            id,
                            invalidTransferTag,
                            invalidOption,
                            PendingMessageState.PREPARING.value
                        )
                        emit(pendingMessage.copy(state = PendingMessageState.PREPARING.value))
                    }

                    else -> {
                        emit(pendingMessage)
                    }
                }
            }
        }
    }

    /**
     * If a transfer has been resumed, its tag is not the same as the initial one.
     * This method gets the transfer with the new tag if exists with the pending message identifier.
     *
     * @param id Pending message id from which the transfer has to be found.
     * @return The transfer if exist, null otherwise.
     */
    private suspend fun findTransferFromPendingMessage(id: Long): Transfer? {
        getTransferDataUseCase()?.let { transferData ->
            transferData.uploadTags.forEach { tag ->
                getTransferByTagUseCase(tag)?.let { transfer ->
                    if (transfer.transferType == TransferType.CHAT_UPLOAD && !transfer.isVoiceClip() && id == transfer.pendingMessageId()) {
                        return transfer
                    }
                }
            }
        }

        return null
    }
}