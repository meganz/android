package mega.privacy.android.app.di.backups

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.DefaultMonitorBackupFolder
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import javax.inject.Singleton

/**
 * Backups module
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupsModule {

    /**
     * Binds the default implementation to [MonitorBackupFolder]
     */
    @Binds
    @Singleton
    abstract fun bindMonitorBackupFolder(implementation: DefaultMonitorBackupFolder): MonitorBackupFolder
}