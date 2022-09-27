package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.di.notification.getMonitorStorageStateEvent
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent

/**
 * Extension function to get the value of state flow from [MonitorStorageStateEvent]
 */
fun MonitorStorageStateEvent.getState(): StorageState =
    storageState.value.storageState

/**
 * Helper function to get latest storage state by calling DI.
 * This method is useful for non-Android classes that Hilt does not support.
 */
fun getStorageState(): StorageState =
    getMonitorStorageStateEvent().getState()
