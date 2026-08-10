package mega.privacy.android.domain.usecase.node.root

import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.entity.node.root.RefreshEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case for monitoring fetch nodes finish.
 */
class MonitorRootNodeRefreshEventUseCase @Inject constructor(private val notificationsRepository: NotificationsRepository) {

    /**
     * Invoke.
     *
     * @return Flow of Boolean.
     */
    operator fun invoke() =
        notificationsRepository.monitorSdkReloadNeeded().map { RefreshEvent.SdkReload }
}