package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.MiscFlagsReadyEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case to monitor the [MiscFlagsReadyEvent] emitted by the SDK (EVENT_MISC_FLAGS_READY).
 *
 * Unlike [MonitorMiscLoadedUseCase], which observes the in-memory [MiscLoadedState], this emits
 * once per actual SDK event, so it also fires for misc-flag reloads that happen after login.
 */
class MonitorMiscFlagsReadyEventUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke the use case.
     *
     * @return Flow that emits whenever the SDK signals that misc flags are ready.
     */
    operator fun invoke(): Flow<Unit> = notificationsRepository.monitorEvent()
        .filterIsInstance<MiscFlagsReadyEvent>()
        .map { }
}
