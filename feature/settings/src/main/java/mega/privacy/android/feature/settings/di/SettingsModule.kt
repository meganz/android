package mega.privacy.android.feature.settings.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.settings.navigation.SettingsFeatureGraph
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class SettingsModule {

    @Provides
    @IntoSet
    fun provideSettingsFeatureDestination(): FeatureDestination = SettingsFeatureGraph()
}
