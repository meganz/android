package mega.privacy.android.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.logout.LogoutTask
import mega.privacy.android.feature.sync.domain.usecase.logout.ClearSyncSolvedIssuesLogoutTask
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.MonitorSyncsUseCaseImpl

@Module
@InstallIn(SingletonComponent::class)
internal interface SyncDomainModule {

    /**
     * Binds the [MonitorSyncsUseCase] to the [MonitorSyncsUseCaseImpl]
     *
     */
    @Binds
    fun bindMonitorSyncsUseCase(impl: MonitorSyncsUseCaseImpl): MonitorSyncsUseCase

    companion object {
        @Provides
        @IntoSet
        fun provideClearSyncSolvedIssuesLogoutTask(task: ClearSyncSolvedIssuesLogoutTask): LogoutTask =
            task
    }
}
