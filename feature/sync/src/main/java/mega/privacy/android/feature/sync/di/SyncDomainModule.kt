package mega.privacy.android.feature.sync.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.logout.LogoutTask
import mega.privacy.android.feature.sync.domain.usecase.logout.ClearSyncSolvedIssuesLogoutTask

@Module
@InstallIn(SingletonComponent::class)
internal interface SyncDomainModule {

    companion object {
        @Provides
        @IntoSet
        fun provideClearSyncSolvedIssuesLogoutTask(task: ClearSyncSolvedIssuesLogoutTask): LogoutTask =
            task
    }
}