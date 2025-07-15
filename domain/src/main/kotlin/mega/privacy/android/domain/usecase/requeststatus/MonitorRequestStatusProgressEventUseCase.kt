package mega.privacy.android.domain.usecase.requeststatus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.RequestStatusProgressEvent
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
        .filterIsInstance<RequestStatusProgressEvent>()
        .map {
            if (it.progress > -1L) {
                Progress(current = it.progress, total = 1000)
            } else {
                null
            }
        }
}