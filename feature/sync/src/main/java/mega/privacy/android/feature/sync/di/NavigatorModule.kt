package mega.privacy.android.feature.sync.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import mega.privacy.android.feature.sync.ui.navigator.SyncNavigator
import mega.privacy.android.feature.sync.ui.navigator.SyncNavigatorImpl

@Module
@InstallIn(ActivityComponent::class)
internal interface NavigatorModule {

    @Binds
    fun bindSyncNavigator(impl: SyncNavigatorImpl): SyncNavigator
}
