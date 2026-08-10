package mega.privacy.android.feature.transfers.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.transfers.navigation.TransfersFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class TransfersModule {

    @Provides
    @IntoSet
    fun provideTransfersFeatureDestination(): FeatureDestination = TransfersFeatureDestination()

}