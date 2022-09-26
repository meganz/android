package mega.privacy.android.app.di.notification

import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors.fromApplication
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.domain.usecase.DefaultMonitorStorageStateEvent
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StorageStateUseCase {

    @Singleton
    @Binds
    abstract fun bindStorageStateEventMonitor(implementation: DefaultMonitorStorageStateEvent): MonitorStorageStateEvent
}

/**
 * This method is to inject MonitorStorageStateEvent into non-Android classes by Hilt
 */
fun getMonitorStorageStateEvent(): MonitorStorageStateEvent = fromApplication(
    MegaApplication.getInstance(),
    StorageStateEventMonitorEntryPoint::class.java).monitorStorageStateEvent

/**
 * This interface is needed to inject MonitorStorageStateEvent by Hilt
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageStateEventMonitorEntryPoint {
    var monitorStorageStateEvent: MonitorStorageStateEvent
}





