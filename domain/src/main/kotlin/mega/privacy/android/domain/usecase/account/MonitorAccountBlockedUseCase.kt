package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.filterIsInstance
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case for monitoring blocked account.
 */
class MonitorAccountBlockedUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {

    /**
     * Invoke.
     *
     * @return Flow of [AccountBlockedEvent]
     */
    operator fun invoke() = notificationsRepository.monitorEvent()
        .filterIsInstance<AccountBlockedEvent>()
}