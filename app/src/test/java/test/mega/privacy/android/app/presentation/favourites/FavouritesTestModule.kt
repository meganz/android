package test.mega.privacy.android.app.presentation.favourites

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.GetTypedNodeModule
import mega.privacy.android.app.di.MapperModule
import mega.privacy.android.app.di.MegaUtilModule
import mega.privacy.android.app.di.homepage.favourites.OpenFileModule
import mega.privacy.android.app.di.photos.PhotosUseCases
import mega.privacy.android.app.di.sortorder.SortOrderUseCases
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.OpenFileWrapper
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.mapper.FavouriteMapper
import mega.privacy.android.data.wrapper.DateUtilWrapper
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.favourites.GetAllFavoritesUseCase
import mega.privacy.android.domain.usecase.favourites.GetFavouriteFolderInfoUseCase
import mega.privacy.android.domain.usecase.favourites.MapFavouriteSortOrderUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetPreviewUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.GetThumbnailUseCase
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [MegaUtilModule::class, OpenFileModule::class, MapperModule::class, SortOrderUseCases::class, PhotosUseCases::class, GetTypedNodeModule::class]
)
object FavouritesTestModule {
    val getAllFavoritesUseCase = mock<GetAllFavoritesUseCase>()
    val getFavouriteFolderInfoUseCase = mock<GetFavouriteFolderInfoUseCase>()
    val stringUtilWrapper = mock<StringUtilWrapper>()
    val favouriteMapper = mock<FavouriteMapper> {
        on { invoke(any(), any(), any(), any(), any(), any()) }.thenReturn(mock())
    }
    val getThumbnailUseCase = mock<GetThumbnailUseCase>()
    val megaUtilWrapper = mock<MegaUtilWrapper>()

    @Provides
    fun provideStringUtilWrapper(): StringUtilWrapper = stringUtilWrapper

    @Provides
    fun provideMegaUtilWrapper(): MegaUtilWrapper = megaUtilWrapper

    @Provides
    fun provideOpenFileWrapper(): OpenFileWrapper = mock()

    @Provides
    fun provideFavouriteMapper(): FavouriteMapper = favouriteMapper

    @Provides
    fun provideGetThumbnail(): GetThumbnailUseCase = getThumbnailUseCase

    @Provides
    fun provideGetPreview(): GetPreviewUseCase = mock()

    @Provides
    fun provideDownloadThumbnail(): DownloadThumbnailUseCase = mock()

    @Provides
    fun provideDownloadPreview(): DownloadPreviewUseCase = mock()

    @Provides
    fun provideGetTimelinePhotosUseCase(): GetTimelinePhotosUseCase = mock()

    @Provides
    fun provideFilterCameraUploadPhotos(): FilterCameraUploadPhotos = mock()

    @Provides
    fun provideFilterCloudDrivePhotos(): FilterCloudDrivePhotos = mock()

    @Provides
    fun provideSetInitialCUPreferences(): SetInitialCUPreferences = mock()

    @Provides
    fun provideGetNodeListByIds(): GetNodeListByIds = mock()

    @Provides
    fun provideMapFavouriteSortOrder(): MapFavouriteSortOrderUseCase = mock()

    @Provides
    fun provideDateUtilWrapper(): DateUtilWrapper = mock()
}
