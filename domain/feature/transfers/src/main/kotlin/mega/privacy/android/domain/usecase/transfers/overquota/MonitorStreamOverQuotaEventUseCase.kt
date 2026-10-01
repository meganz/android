package mega.privacy.android.domain.usecase.transfers.overquota

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.StreamOverQuotaEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Monitor bandwidth over quota hit while streaming media.
 *
 * Streaming reads bypass the transfer subsystem, so they never raise a transfer temporary error.
 * The SDK reports them through a dedicated global event instead.
 */
class MonitorStreamOverQuotaEventUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * Invoke
     *
     * @return a [Flow] emitting the time left until the over quota state ends, each time a
     * streaming over quota event occurs.
     */
    operator fun invoke(): Flow<Duration> = notificationsRepository.monitorEvent()
        .filterIsInstance<StreamOverQuotaEvent>()
        .mapNotNull { event -> event.timeLeft.takeIf { it.isPositive() } }
}
