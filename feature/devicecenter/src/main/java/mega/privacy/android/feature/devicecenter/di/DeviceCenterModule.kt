package mega.privacy.android.feature.devicecenter.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.devicecenter.navigation.DeviceCenterFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class DeviceCenterModule {

    @Provides
    @IntoSet
    fun provideDeviceCenterFeatureDestination(): FeatureDestination =
        DeviceCenterFeatureDestination()
}
