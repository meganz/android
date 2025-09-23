package mega.privacy.android.app.di.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.activities.destinations.LegacyCoreActivityFeatureGraph
import mega.privacy.android.app.activities.destinations.LegacyCoreFragmentFeatureGraph
import mega.privacy.android.app.presentation.filecontact.navigation.FileContactFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class FeatureDestinationModule {

    @Provides
    @IntoSet
    fun provideLegacyCoreActivityFeatureDestination(): FeatureDestination =
        LegacyCoreActivityFeatureGraph()

    @Provides
    @IntoSet
    fun provideLegacyCoreFragmentFeatureDestination(): FeatureDestination =
        LegacyCoreFragmentFeatureGraph()

    @Provides
    @IntoSet
    fun provideFileContactFeatureDestination(): FeatureDestination = FileContactFeatureDestination()
}