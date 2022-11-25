package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.album.AlbumUseCases
import mega.privacy.android.domain.usecase.AddPhotosToAlbum
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.GetUserAlbums
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [AlbumUseCases::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module
object TestAlbumUseCases {
    @Provides
    fun provideGetUserAlbums(): GetUserAlbums = mock()

    @Provides
    fun provideGetAlbumPhotos(): GetAlbumPhotos = mock()

    @Provides
    fun provideGetUserAlbum(): GetUserAlbum = mock()

    @Provides
    fun provideAddPhotosToAlbum(): AddPhotosToAlbum = mock()
}
