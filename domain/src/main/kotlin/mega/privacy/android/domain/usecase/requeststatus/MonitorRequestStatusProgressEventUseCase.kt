package mega.privacy.android.domain.usecase.requeststatus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case to monitor EVENT_REQSTAT_PROGRESS events
 */
class MonitorRequestStatusProgressEventUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * The flow of [Progress]
     */
    operator fun invoke(): Flow<Progress?> = notificationsRepository
        .monitorEvent()
        .filter { it.type == EventType.RequestStatusProgress }
        .map {
            if (it.number > -1L) {
                Progress(current = it.number, total = 1000)
            } else {
                null
            }
        }
}