package mega.privacy.android.core.nodecomponents.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.core.nodecomponents.navigation.NodeComponentsFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class NodeComponentsModule {
    @Provides
    @IntoSet
    fun provideNodeComponentsFeatureDestination(): FeatureDestination =
        NodeComponentsFeatureDestination()
}