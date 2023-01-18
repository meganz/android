package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.StorageStateEvent

/**
 * Use case to monitor the latest state of [StorageStateEvent]
 */
fun interface MonitorStorageStateEvent {
    /**
     *
     * The state flow of [StorageStateEvent]
     */
    operator fun invoke(): StateFlow<StorageStateEvent>
}