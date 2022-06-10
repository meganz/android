package mega.privacy.android.app.di.homepage.favourites

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

/**
 * Provide implementation for use cases that are regarding favourites feature.
 */
@Module
@InstallIn(ViewModelComponent::class)
abstract class FavouritesUseCases {

    /**
     * Provide GetAllFavourites implementation
     */
    @Binds
    abstract fun bindGetAllFavorites(useCase: DefaultGetAllFavorites): GetAllFavorites

    /**
     * Provide GetFavouriteFolderInfo implementation
     */
    @Binds
    abstract fun bindGetFavouriteFolderInfo(useCase: DefaultGetFavouriteFolderInfo): GetFavouriteFolderInfo

    /**
     * Provide RemoveFavourites implementation
     */
    @Binds
    abstract fun bindRemoveFavourites(useCase: DefaultRemoveFavourites): RemoveFavourites
}