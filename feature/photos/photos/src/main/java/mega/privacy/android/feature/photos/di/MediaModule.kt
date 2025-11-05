package mega.privacy.android.feature.photos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.photos.navigation.MediaFeatureDestination
import mega.privacy.android.feature.photos.navigation.MediaNavItem
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem


@Module
@InstallIn(SingletonComponent::class)
class MediaModule {

    @Provides
    @IntoSet
    fun provideMediaNavItem(): MainNavItem = MediaNavItem()

    @Provides
    @IntoSet
    fun provideMediaFeatureDestination(): FeatureDestination = MediaFeatureDestination()
}