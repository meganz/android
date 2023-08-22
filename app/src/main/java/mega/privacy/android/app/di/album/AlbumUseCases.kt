package mega.privacy.android.app.di.album

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.presentation.photos.albums.model.mapper.DefaultUIAlbumMapper
import mega.privacy.android.app.presentation.photos.albums.model.mapper.LegacyUIAlbumMapper
import mega.privacy.android.domain.usecase.AddPhotosToAlbum
import mega.privacy.android.domain.usecase.DefaultAddPhotosToAlbum
import mega.privacy.android.domain.usecase.DefaultGetAlbumPhotos
import mega.privacy.android.domain.usecase.DefaultGetUserAlbum
import mega.privacy.android.domain.usecase.DefaultGetUserAlbums
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbum
import mega.privacy.android.domain.usecase.GetUserAlbums

@Module
@InstallIn(ViewModelComponent::class)
abstract class AlbumUseCases {
    @Binds
    abstract fun bindGetUserAlbums(useCase: DefaultGetUserAlbums): GetUserAlbums

    @Binds
    abstract fun bindGetAlbumPhotos(useCase: DefaultGetAlbumPhotos): GetAlbumPhotos

    @Binds
    abstract fun bindGetUserAlbum(useCase: DefaultGetUserAlbum): GetUserAlbum

    @Binds
    abstract fun bindAddPhotosToAlbum(useCase: DefaultAddPhotosToAlbum): AddPhotosToAlbum

    /**
     * Binds UIAlbum mapper
     */
    @Binds
    abstract fun bindUIAlbumMapper(useCase: DefaultUIAlbumMapper): LegacyUIAlbumMapper
}
