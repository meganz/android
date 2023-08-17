package mega.privacy.android.domain.usecase.createaccount

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case for monitoring an account confirmation.
 */
class MonitorAccountConfirmationUseCase @Inject constructor(private val notificationsRepository: NotificationsRepository) {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = notificationsRepository.monitorEvent()
        .filter { event -> event.type == EventType.AccountConfirmation }
        .map { true }
}