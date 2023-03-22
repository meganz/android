package mega.privacy.android.app.di.notification

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors.fromApplication
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase

/**
 * This method is to inject MonitorStorageStateEvent into non-Android classes by Hilt
 */
fun getMonitorStorageStateEvent(): MonitorStorageStateEventUseCase = fromApplication(
    MegaApplication.getInstance(),
    StorageStateEventMonitorEntryPoint::class.java
).monitorStorageStateEvent

/**
 * This interface is needed to inject MonitorStorageStateEvent by Hilt
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageStateEventMonitorEntryPoint {
    var monitorStorageStateEvent: MonitorStorageStateEventUseCase
}





