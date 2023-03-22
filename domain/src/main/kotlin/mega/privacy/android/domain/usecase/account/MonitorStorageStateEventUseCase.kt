package mega.privacy.android.domain.usecase.account

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.stateIn
import mega.privacy.android.domain.entity.EventType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.StorageStateEvent
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.repository.NotificationsRepository
import javax.inject.Inject

/**
 * Use case to monitor the latest state of [StorageStateEvent]
 */
class MonitorStorageStateEventUseCase @Inject constructor(
    private val notificationsRepository: NotificationsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {

    private val events: StateFlow<StorageStateEvent> by lazy {
        notificationsRepository
            .monitorEvent()
            .filterIsInstance<StorageStateEvent>()
            .stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = StorageStateEvent(
                    handle = 1L,
                    eventString = "",
                    number = 0L,
                    text = "",
                    type = EventType.Storage,
                    storageState = StorageState.Unknown  // initial state is [StorageState.Unknown]
                )
            )
    }

    /**
     *
     * The state flow of [StorageStateEvent]
     */
    operator fun invoke() = events
}