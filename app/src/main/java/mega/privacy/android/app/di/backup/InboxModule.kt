package mega.privacy.android.app.di.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.DefaultMonitorBackupFolder
import mega.privacy.android.domain.usecase.MonitorBackupFolder

/**
 * Inbox module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InboxModule {

    /**
     * Binds the default implementation to [MonitorBackupFolder]
     */
    @Binds
    abstract fun bindMonitorBackupFolder(implementation: DefaultMonitorBackupFolder): MonitorBackupFolder
}