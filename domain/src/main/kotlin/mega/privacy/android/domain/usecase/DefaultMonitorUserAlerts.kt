package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Default monitor user alerts
 *
 * @property notificationsRepository
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultMonitorUserAlerts @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
) : MonitorUserAlerts {
    override fun invoke() =
        notificationsRepository.monitorUserAlerts().runningFold(
            initial = runBlocking { notificationsRepository.getUserAlerts() },
            operation = { current, updates ->
                (updates.filterNot { it.isOwnChange } + current).distinctBy { it.id }
            }
        ).distinctUntilChanged()
            .mapLatest { list -> list.sortedByDescending { it.createdTime } }
}