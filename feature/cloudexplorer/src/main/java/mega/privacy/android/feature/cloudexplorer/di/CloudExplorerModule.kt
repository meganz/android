package mega.privacy.android.feature.cloudexplorer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.cloudexplorer.navigation.CloudExplorerFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class CloudExplorerModule {

    @Provides
    @IntoSet
    fun provideCloudExplorerFeatureDestination(): FeatureDestination =
        CloudExplorerFeatureDestination()
}
