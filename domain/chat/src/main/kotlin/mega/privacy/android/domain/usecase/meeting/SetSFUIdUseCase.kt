package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * Use case to set the SFU ID.
 */
class SetSFUIdUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {
    /**
     * Sets the SFU ID
     * @param sfuId The SFU ID to set.
     */
    suspend operator fun invoke(sfuId: Int) = chatRepository.setSFUid(sfuId)
}
