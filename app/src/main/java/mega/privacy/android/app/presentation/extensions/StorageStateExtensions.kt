package mega.privacy.android.app.presentation.extensions

import android.content.Context
import dagger.hilt.android.EntryPointAccessors.fromApplication
import mega.privacy.android.app.di.notification.StorageStateEventMonitorEntryPoint
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase

/**
 * Extension function to get the value of state flow from [MonitorStorageStateEventUseCase]
 */
internal fun MonitorStorageStateEventUseCase.getState(): StorageState =
    invoke().value.storageState

/**
 * Helper function to get latest storage state by calling DI.
 * This method is useful for non-Android classes that Hilt does not support.
 */
internal fun getStorageState(context: Context): StorageState =
    fromApplication(
        context,
        StorageStateEventMonitorEntryPoint::class.java
    ).monitorStorageStateEvent.getState()
