package mega.privacy.android.domain.usecase.chat

import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case for getting the node ID of "My chat files" folder from local storage.
 * Returns null if the folder handle has not been stored yet (i.e. INVALID_HANDLE).
 */
class GetChatFilesFolderIdUseCase @Inject constructor(private val chatRepository: ChatRepository) {
    /**
     * Invoke
     *
     * @return [NodeId] of "My chat files" folder, or null if not set.
     */
    suspend operator fun invoke(): NodeId? = chatRepository.getChatFilesFolderId()
}
