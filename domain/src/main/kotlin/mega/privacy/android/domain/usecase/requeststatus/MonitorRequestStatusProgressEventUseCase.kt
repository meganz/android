package mega.privacy.android.domain.usecase.requeststatus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import mega.privacy.android.domain.entity.Event
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case to monitor EVENT_REQSTAT_PROGRESS events
 */
class MonitorRequestStatusProgressEventUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {
    /**
     * The flow of [Event]
     */
    operator fun invoke(): Flow<Event> = notificationsRepository
        .monitorEvent()
        .filter {
            it.type == EventType.RequestStatusProgress
        }
}