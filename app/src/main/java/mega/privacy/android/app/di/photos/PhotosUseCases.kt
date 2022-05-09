package mega.privacy.android.app.di.photos

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class PhotosUseCases {

    @Binds
    abstract fun bindGetAlbums(useCase: DefaultGetAlbums): GetAlbums

    @Binds
    abstract fun bindGetThumbnailFromServer(useCase: DefaultGetThumbnail): GetThumbnail
}