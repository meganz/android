package mega.privacy.android.feature.photos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import mega.privacy.android.feature.photos.navigation.PhotosFeatureGraph
import mega.privacy.android.feature.photos.navigation.PhotosNavItem
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem


@Module
@InstallIn(SingletonComponent::class)
class PhotosModule {

    @Provides
    @IntoSet
    fun providePhotosNavItem(): MainNavItem = PhotosNavItem()

    @Provides
    @IntoSet
    fun providePhotosFeatureDestination(): FeatureDestination = PhotosFeatureGraph()
}