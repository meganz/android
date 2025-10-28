package mega.privacy.android.feature.photos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.feature.photos.provider.AlbumsDataProvider
import mega.privacy.android.feature.photos.provider.implementation.SystemAlbumsDataProvider
import mega.privacy.android.feature.photos.provider.implementation.UserAlbumsDataProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AlbumsModule {
    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideAlbumsDataProvider(
        systemAlbumsDataProvider: SystemAlbumsDataProvider,
        userAlbumsDataProvider: UserAlbumsDataProvider
    ): Set<@JvmSuppressWildcards AlbumsDataProvider> =
        setOf(
            systemAlbumsDataProvider,
            userAlbumsDataProvider
        )
}