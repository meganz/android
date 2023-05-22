package mega.privacy.android.domain.usecase.notifications

import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case for monitoring changes in the home badge count.
 */
class MonitorHomeBadgeCountUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {

    /**
     * Invoke.
     *
     * @return Flow of [Int].
     */
    operator fun invoke() = notificationsRepository.monitorHomeBadgeCount()
}