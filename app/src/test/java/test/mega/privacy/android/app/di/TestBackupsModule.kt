package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.backups.BackupsModule
import mega.privacy.android.domain.usecase.MonitorBackupFolder
import org.mockito.kotlin.mock

/**
 * Provides test dependencies for the Backups Module
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [BackupsModule::class],
)
object TestBackupsModule {

    @Provides
    fun provideMonitorBackupFolder(): MonitorBackupFolder = mock()
}