package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.AccountBlockedEvent
import mega.privacy.android.domain.entity.account.AccountBlockedType
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
    operator fun invoke() =
        flow {
            emit(AccountBlockedEvent(-1L, AccountBlockedType.NOT_BLOCKED, ""))
            emitAll(notificationsRepository.monitorEvent().filterIsInstance<AccountBlockedEvent>())
        }

}