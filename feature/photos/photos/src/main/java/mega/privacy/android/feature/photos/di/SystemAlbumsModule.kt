package mega.privacy.android.feature.photos.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ElementsIntoSet
import mega.privacy.android.domain.entity.media.SystemAlbum
import mega.privacy.android.feature.photos.model.FavouriteSystemAlbum
import mega.privacy.android.feature.photos.model.GifSystemAlbum
import mega.privacy.android.feature.photos.model.RawSystemAlbum
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SystemAlbumsModule {
    @Provides
    @Singleton
    @ElementsIntoSet
    fun provideSystemAlbums(
        gifSystemAlbumType: GifSystemAlbum,
        rawSystemAlbumType: RawSystemAlbum,
        favouriteSystemAlbumType: FavouriteSystemAlbum,
    ): Set<@JvmSuppressWildcards SystemAlbum> = setOf(
        gifSystemAlbumType,
        rawSystemAlbumType,
        favouriteSystemAlbumType
    )
}