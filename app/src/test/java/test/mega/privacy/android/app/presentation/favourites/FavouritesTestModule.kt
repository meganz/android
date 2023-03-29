package test.mega.privacy.android.app.presentation.favourites

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.GetTypedNodeModule
import mega.privacy.android.app.di.MapperModule
import mega.privacy.android.app.di.MegaUtilModule
import mega.privacy.android.app.di.homepage.favourites.FavouritesUseCases
import mega.privacy.android.app.di.homepage.favourites.OpenFileModule
import mega.privacy.android.app.di.photos.PhotosUseCases
import mega.privacy.android.app.di.sortorder.SortOrderUseCases
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.domain.usecase.DefaultMapFavouriteSortOrder
import mega.privacy.android.domain.usecase.DownloadPreview
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.EnablePhotosCameraUpload
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFavouriteFolderInfo
import mega.privacy.android.domain.usecase.GetFavouriteSortOrder
import mega.privacy.android.domain.usecase.GetOfflineFile
import mega.privacy.android.domain.usecase.GetPreview
import mega.privacy.android.domain.usecase.GetThumbnail
import mega.privacy.android.domain.usecase.GetTimelinePhotos
import mega.privacy.android.domain.usecase.IsAvailableOffline
import mega.privacy.android.domain.usecase.IsCameraSyncPreferenceEnabled
import mega.privacy.android.domain.usecase.MapFavouriteSortOrder
import mega.privacy.android.domain.usecase.RemoveFavourites
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [FavouritesUseCases::class, MegaUtilModule::class, OpenFileModule::class,
        MapperModule::class, SortOrderUseCases::class, PhotosUseCases::class, GetTypedNodeModule::class]
)
object FavouritesTestModule {
    val getAllFavourites = mock<GetAllFavorites>()
    val getFavouriteFolderInfo = mock<GetFavouriteFolderInfo>()
    val stringUtilWrapper = mock<StringUtilWrapper>()
    val favouriteMapper = mock<FavouriteMapper> {
        on { invoke(any(), any(), any(), any(), any(), any()) }.thenReturn(mock())
    }
    val getThumbnail = mock<GetThumbnail>()
    val megaUtilWrapper = mock<MegaUtilWrapper>()
    val isAvailableOffline = mock<IsAvailableOffline>{ onBlocking { invoke(any()) }.thenReturn(false)}

    @Provides
    fun provideGetAllFavorites(): GetAllFavorites = getAllFavourites

    @Provides
    fun provideStringUtilWrapper(): StringUtilWrapper = stringUtilWrapper

    @Provides
    fun provideGetFavouriteFolderInfo(): GetFavouriteFolderInfo = getFavouriteFolderInfo

    @Provides
    fun provideMegaUtilWrapper(): MegaUtilWrapper = megaUtilWrapper

    @Provides
    fun provideOpenFileWrapper(): OpenFileWrapper = mock()

    @Provides
    fun provideRemoveFavourites(): RemoveFavourites = mock()

    @Provides
    fun provideFavouriteMapper(): FavouriteMapper = favouriteMapper

    @Provides
    fun provideGetThumbnail(): GetThumbnail = getThumbnail

    @Provides
    fun provideGetPreview(): GetPreview = mock()

    @Provides
    fun provideDownloadThumbnail(): DownloadThumbnail = mock()

    @Provides
    fun provideDownloadPreview(): DownloadPreview = mock()

    @Provides
    fun provideGetTimelinePhotos(): GetTimelinePhotos = mock()

    @Provides
    fun provideFilterCameraUploadPhotos(): FilterCameraUploadPhotos = mock()

    @Provides
    fun provideFilterCloudDrivePhotos(): FilterCloudDrivePhotos = mock()

    @Provides
    fun provideEnableCameraUpload(): EnablePhotosCameraUpload = mock()

    @Provides
    fun provideSetInitialCUPreferences(): SetInitialCUPreferences = mock()

    @Provides
    fun provideGetNodeListByIds(): GetNodeListByIds = mock()

    @Provides
    fun provideIsCameraSyncPreferenceEnabled(): IsCameraSyncPreferenceEnabled = mock()

    @Provides
    fun provideGetFavouriteSortOrder(
        getSortOrder: GetCloudSortOrder,
        mapFavouriteSortOrder: MapFavouriteSortOrder,
    ): GetFavouriteSortOrder =
        GetFavouriteSortOrder { mapFavouriteSortOrder(getSortOrder()) }

    @Provides
    fun provideMapFavouriteSortOrder(): MapFavouriteSortOrder = DefaultMapFavouriteSortOrder()

    @Provides
    fun provideIsAvailableOffline(): IsAvailableOffline = isAvailableOffline

    @Provides
    fun provideGetOfflineFile(): GetOfflineFile = mock()
}