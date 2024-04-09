package mega.privacy.android.domain.usecase.meeting

import mega.privacy.android.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * use case for monitoring upgrade dialog is closed
 */
class MonitorUpgradeDialogClosedUseCase @Inject constructor(
    private val chatRepository: ChatRepository,
) {

    /**
     * Invoke function
     */
    operator fun invoke() = chatRepository.monitorUpgradeDialogClosed()
}
