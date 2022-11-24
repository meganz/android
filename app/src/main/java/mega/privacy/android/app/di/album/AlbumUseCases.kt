package mega.privacy.android.app.di.album

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.usecase.DefaultGetAlbumPhotos
import mega.privacy.android.domain.usecase.DefaultGetUserAlbums
import mega.privacy.android.domain.usecase.GetAlbumPhotos
import mega.privacy.android.domain.usecase.GetUserAlbums

@Module
@InstallIn(ViewModelComponent::class)
abstract class AlbumUseCases {
    @Binds
    abstract fun bindGetUserAlbums(useCase: DefaultGetUserAlbums): GetUserAlbums

    @Binds
    abstract fun bindGetAlbumPhotos(useCase: DefaultGetAlbumPhotos): GetAlbumPhotos
}
