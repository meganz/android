package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * use case for broadcasting event when upgrade dialog is closed
 */
class BroadcastUpgradeDialogClosedUseCase @Inject constructor(private val chatRepository: ChatRepository) {

    /**
     * Broadcast the upgrade dialog is closed
     */
    suspend operator fun invoke() = chatRepository.broadcastUpgradeDialogClosed()
}
