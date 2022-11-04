package mega.privacy.android.app.di.homepage.favourites

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.usecase.DefaultHasAncestor
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.usecase.AddNodeType
import mega.privacy.android.domain.usecase.DefaultAddNodeType
import mega.privacy.android.domain.usecase.DefaultGetAllFavorites
import mega.privacy.android.domain.usecase.DefaultGetDeviceType
import mega.privacy.android.domain.usecase.DefaultGetFavouriteFolderInfo
import mega.privacy.android.domain.usecase.DefaultGetFolderType
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetDeviceType
import mega.privacy.android.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.domain.usecase.GetFolderType
import mega.privacy.android.domain.usecase.HasAncestor
import mega.privacy.android.domain.usecase.RemoveFavourites

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


    companion object {
        /**
         * Provide RemoveFavourites implementation
         */
        @Provides
        fun provideRemoveFavourites(favouritesRepository: FavouritesRepository): RemoveFavourites =
            RemoveFavourites(favouritesRepository::removeFavourites)
    }
}