package mega.privacy.android.app.di.homepage.favourites

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.usecase.DefaultGetAllFavorites
import mega.privacy.android.domain.usecase.DefaultGetFavouriteFolderInfo
import mega.privacy.android.domain.usecase.DefaultGetFavouriteSortOrder
import mega.privacy.android.domain.usecase.DefaultGetOfflineFile
import mega.privacy.android.domain.usecase.DefaultIsAvailableOffline
import mega.privacy.android.domain.usecase.DefaultMapFavouriteSortOrder
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.domain.usecase.GetFavouriteSortOrder
import mega.privacy.android.domain.usecase.GetOfflineFile
import mega.privacy.android.domain.usecase.IsAvailableOffline
import mega.privacy.android.domain.usecase.MapFavouriteSortOrder
import mega.privacy.android.domain.usecase.RemoveFavourites

/**
 * Provide implementation for use cases that are regarding favourites feature.
 */
@Module
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
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

    @Binds
    abstract fun bindGetFavouriteSortOrder(implementation: DefaultGetFavouriteSortOrder): GetFavouriteSortOrder

    @Binds
    abstract fun bindMapFavouriteSortOrder(implementation: DefaultMapFavouriteSortOrder): MapFavouriteSortOrder

    @Binds
    abstract fun bindIsAvailableOffline(implementation: DefaultIsAvailableOffline): IsAvailableOffline

    @Binds
    abstract fun bindGetOfflineFile(implementation: DefaultGetOfflineFile): GetOfflineFile


    companion object {
        /**
         * Provide RemoveFavourites implementation
         */
        @Provides
        fun provideRemoveFavourites(favouritesRepository: FavouritesRepository): RemoveFavourites =
            RemoveFavourites(favouritesRepository::removeFavourites)
    }
}