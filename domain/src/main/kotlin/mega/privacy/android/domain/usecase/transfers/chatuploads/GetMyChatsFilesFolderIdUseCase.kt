package mega.privacy.android.domain.usecase.transfers.chatuploads

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

class GetMyChatsFilesFolderIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * this use-case will be properly implemented in AND-17995, now it will throw a NotImplementedError
     */
    suspend operator fun invoke() = chatRepository.getMyChatsFilesFolderId()
}