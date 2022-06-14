package test.mega.privacy.android.app.presentation.favourites

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.MapperModule
import mega.privacy.android.app.di.MegaUtilModule
import mega.privacy.android.app.di.homepage.favourites.FavouritesUseCases
import mega.privacy.android.app.di.homepage.favourites.OpenFileModule
import mega.privacy.android.app.di.photos.PhotosUseCases
import mega.privacy.android.app.di.sortorder.SortOrderUseCases
import mega.privacy.android.app.domain.usecase.GetAlbums
import mega.privacy.android.app.domain.usecase.GetAllFavorites
import mega.privacy.android.app.domain.usecase.GetCameraSortOrder
import mega.privacy.android.app.domain.usecase.GetCloudSortOrder
import mega.privacy.android.app.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.app.domain.usecase.GetThumbnail
import mega.privacy.android.app.domain.usecase.RemoveFavourites
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.mapper.FavouriteMapper
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FavouritesUseCases::class, MegaUtilModule::class, OpenFileModule::class,
        MapperModule::class, SortOrderUseCases::class, PhotosUseCases::class]
)
object FavouritesTestModule {
    val getAllFavourites = mock<GetAllFavorites>()
    val getFavouriteFolderInfo = mock<GetFavouriteFolderInfo>()
    val stringUtilWrapper = mock<StringUtilWrapper>()
    val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val getCameraSortOrder = mock<GetCameraSortOrder>()
    val favouriteMapper = mock<FavouriteMapper>()
    val getThumbnail = mock<GetThumbnail>()

    @Provides
    fun provideGetAllFavorites(): GetAllFavorites = getAllFavourites

    @Provides
    fun provideStringUtilWrapper(): StringUtilWrapper = stringUtilWrapper

    @Provides
    fun provideGetFavouriteFolderInfo(): GetFavouriteFolderInfo = getFavouriteFolderInfo

    @Provides
    fun provideMegaUtilWrapper(): MegaUtilWrapper = mock()

    @Provides
    fun provideOpenFileWrapper(): OpenFileWrapper = mock()

    @Provides
    fun provideRemoveFavourites(): RemoveFavourites = mock()

    @Provides
    fun provideGetCloudSortOrder(): GetCloudSortOrder = getCloudSortOrder

    @Provides
    fun provideGetCameraSortOrder(): GetCameraSortOrder = getCameraSortOrder

    @Provides
    fun provideFavouriteMapper(): FavouriteMapper = favouriteMapper

    @Provides
    fun provideGetThumbnail(): GetThumbnail = getThumbnail

    @Provides
    fun provideGetAlbums(): GetAlbums = mock()
}