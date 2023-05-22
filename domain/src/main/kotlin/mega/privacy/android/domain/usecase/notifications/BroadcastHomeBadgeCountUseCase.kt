package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case for notifying about home badge count changes.
 */
class BroadcastHomeBadgeCountUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {

    /**
     * Invoke.
     *
     * @param badgeCount The new badge count updated.
     */
    suspend operator fun invoke(badgeCount: Int) =
        notificationsRepository.broadcastHomeBadgeCount(badgeCount)
}