package mega.privacy.android.app.di.notification

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase

/**
 * This interface is needed to inject MonitorStorageStateEvent by Hilt
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageStateEventMonitorEntryPoint {
    val monitorStorageStateEvent: MonitorStorageStateEventUseCase
}





