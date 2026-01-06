package mega.privacy.android.feature.sync.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.sync.navigation.SyncFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class SyncNavigationModule {

    @Provides
    @IntoSet
    fun provideSyncFeatureDestination(destination: SyncFeatureDestination): FeatureDestination =
        destination
}
