package mega.privacy.android.domain.usecase.createaccount

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.AccountConfirmationEvent
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
        .filterIsInstance<AccountConfirmationEvent>()
        .map { true }
}