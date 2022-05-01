package mega.privacy.android.app.di.photos.album

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class AlbumsUseCases {

    @Binds
    abstract fun bindGetFavouriteAlbumItems(useCase: DefaultGetFavoriteAlbumItems): GetFavouriteAlbumItems
}