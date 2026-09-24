package mega.privacy.android.shared.nodes.namecollision.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.shared.nodes.namecollision.NameCollisionLaunchSourceHolder

@Module
@InstallIn(SingletonComponent::class)
class NameCollisionNavigationModule {
    @Provides
    @IntoSet
    fun provideNameCollisionFeatureDestination(
        launchSourceHolder: NameCollisionLaunchSourceHolder,
    ): FeatureDestination = NameCollisionFeatureDestination(launchSourceHolder)
}
