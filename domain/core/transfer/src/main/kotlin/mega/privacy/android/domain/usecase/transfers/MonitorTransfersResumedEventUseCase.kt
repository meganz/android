package mega.privacy.android.domain.usecase.transfers

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.TransfersResumedEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case for monitoring transfer resumed events.
 */
class MonitorTransfersResumedEventUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() = notificationsRepository.monitorEvent()
        .filterIsInstance<TransfersResumedEvent>()
        .mapNotNull { event -> event.uniqueIds.takeIf { it.isNotEmpty() } }
}