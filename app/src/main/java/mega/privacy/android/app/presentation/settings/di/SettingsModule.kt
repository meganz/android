package mega.privacy.android.app.presentation.settings.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.presentation.settings.SettingFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class SettingsModule {

    @Provides
    @IntoSet
    fun provideSettingsFeatureDestination(): FeatureDestination = SettingFeatureDestination()

}