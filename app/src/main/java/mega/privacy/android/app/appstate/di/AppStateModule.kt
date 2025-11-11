package mega.privacy.android.app.appstate.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.app.appstate.content.navigation.MainNavigationFeatureDestination
import mega.privacy.android.navigation.contract.FeatureDestination

@Module
@InstallIn(SingletonComponent::class)
class AppStateModule {
    @Provides
    @IntoSet
    fun provideMainNavigationFeatureDestination(): FeatureDestination =
        MainNavigationFeatureDestination()
}