package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.photos.PhotosUseCases
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumsMap
import mega.privacy.android.domain.usecase.GetPhotosByFolderId
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [PhotosUseCases::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module
object TestPhotosUseCases {

    @Provides
    fun provideGetDefaultAlbumPhotos(): GetDefaultAlbumPhotos = mock()

    @Provides
    fun provideGetDefaultAlbumsMap(): GetDefaultAlbumsMap = mock()

    @Provides
    fun provideGetPhotosByFolderId(): GetPhotosByFolderId = mock()

}