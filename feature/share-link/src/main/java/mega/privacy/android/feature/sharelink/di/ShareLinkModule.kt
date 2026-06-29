package mega.privacy.android.feature.sharelink.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.sharelink.navigation.ShareLinkFeatureGraph
import mega.privacy.android.navigation.contract.FeatureDestination

/**
 * Hilt module contributing the share-link feature destinations to the navigation graph.
 */
@Module
@InstallIn(SingletonComponent::class)
class ShareLinkModule {

    @Provides
    @IntoSet
    fun provideShareLinkFeatureDestination(): FeatureDestination = ShareLinkFeatureGraph()
}
