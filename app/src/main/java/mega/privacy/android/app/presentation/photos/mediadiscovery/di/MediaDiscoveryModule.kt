package mega.privacy.android.app.presentation.photos.mediadiscovery.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.photos.mediadiscovery.navigation.LegacyMediaDiscoveryFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class MediaDiscoveryModule {

    @Provides
    @IntoSet
    fun provideLegacyMediaDiscoveryFeatureDestination(): FeatureDestination =
        LegacyMediaDiscoveryFeatureDestination()
}
