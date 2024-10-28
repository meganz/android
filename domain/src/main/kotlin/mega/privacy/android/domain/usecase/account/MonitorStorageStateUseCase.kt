package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case to monitor the latest state of [StorageState]
 */
class MonitorStorageStateUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    private val getCurrentStorageStateUseCase: GetCurrentStorageStateUseCase,
) {

    /**
     *
     * The state flow of [StorageState]
     */
    operator fun invoke() = notificationsRepository.monitorEvent()
        .filterIsInstance<StorageStateEvent>().map {
            it.storageState
        }.onStart { emit(getCurrentStorageStateUseCase()) }
}