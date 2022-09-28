package mega.privacy.android.domain.usecase

import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.StorageStateEvent

/**
 * Use case to monitor the latest state of [StorageStateEvent]
 */
interface MonitorStorageStateEvent {
    /**
     *
     * The state flow of [StorageStateEvent]
     */
    val storageState: StateFlow<StorageStateEvent>
}