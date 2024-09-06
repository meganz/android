package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.logout.ClearChatDataLogoutTask
import mega.privacy.android.domain.usecase.logout.ClearPasscodeDataLogoutTask
import mega.privacy.android.domain.usecase.logout.ClearVideoPlaybackDataLogoutTask
import mega.privacy.android.domain.usecase.logout.LogoutTask
import mega.privacy.android.domain.usecase.logout.RemoveBackupFoldersLogoutTask

@Module
@DisableInstallInCheck
internal abstract class InternalLogoutModule {
    companion object {
        @Provides
        @IntoSet
        fun provideRemoveBackupFoldersLogoutTask(task: RemoveBackupFoldersLogoutTask): LogoutTask =
            task

        @Provides
        @IntoSet
        fun provideClearPasscodeDataLogoutTask(task: ClearPasscodeDataLogoutTask): LogoutTask =
            task

        @Provides
        @IntoSet
        fun provideClearChatDataLogoutTask(task: ClearChatDataLogoutTask): LogoutTask =
            task

        @Provides
        @IntoSet
        fun provideClearVideoPlaybackDataLogoutTask(task: ClearVideoPlaybackDataLogoutTask): LogoutTask =
            task
    }
}