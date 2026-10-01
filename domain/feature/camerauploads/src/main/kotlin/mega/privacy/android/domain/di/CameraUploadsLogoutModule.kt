package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.logout.LogoutTask
import mega.privacy.android.domain.usecase.logout.RemoveBackupFoldersLogoutTask

@Module
@InstallIn(SingletonComponent::class)
internal abstract class CameraUploadsLogoutModule {
    companion object {
        @Provides
        @IntoSet
        fun provideRemoveBackupFoldersLogoutTask(task: RemoveBackupFoldersLogoutTask): LogoutTask =
            task
    }
}
