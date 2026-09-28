package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.domain.usecase.logout.ClearRecentSearchesLogoutTask
import mega.privacy.android.domain.usecase.logout.ClearSortingAndViewModePreferencesLogoutTask
import mega.privacy.android.domain.usecase.logout.LogoutTask

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NodeLogoutModule {
    companion object {
        @Provides
        @IntoSet
        fun provideClearRecentSearchesLogoutTask(task: ClearRecentSearchesLogoutTask): LogoutTask =
            task

        @Provides
        @IntoSet
        fun provideClearSortingAndViewModePreferencesLogoutTask(task: ClearSortingAndViewModePreferencesLogoutTask): LogoutTask =
            task
    }
}
