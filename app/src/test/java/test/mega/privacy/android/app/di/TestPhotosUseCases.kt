package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.photos.PhotosUseCases
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetPhotosByIds
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandleUseCase
import mega.privacy.android.domain.usecase.photos.AddPhotosToAlbumUseCase
import mega.privacy.android.domain.usecase.photos.DisableExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoPreviewUseCase
import mega.privacy.android.domain.usecase.photos.DownloadPublicAlbumPhotoThumbnailUseCase
import mega.privacy.android.domain.usecase.photos.ExportAlbumsUseCase
import mega.privacy.android.domain.usecase.photos.GetPhotosByFolderIdUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodeDataUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumNodesDataUseCase
import mega.privacy.android.domain.usecase.photos.GetPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.ImportPublicAlbumUseCase
import mega.privacy.android.domain.usecase.photos.SaveAlbumToFolderUseCase
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [PhotosUseCases::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
)
@Module
object TestPhotosUseCases {

    @Provides
    fun provideGetDefaultAlbumPhotos(): GetDefaultAlbumPhotos = mock()

    @Provides
    fun provideGetPhotosByFolderIdUseCase(): GetPhotosByFolderIdUseCase = mock()

    @Provides
    fun provideObserveAlbumPhotosAddingProgressUseCase(): ObserveAlbumPhotosAddingProgress = mock()

    @Provides
    fun provideUpdateAlbumPhotosAddingProgressCompletedUseCase(): UpdateAlbumPhotosAddingProgressCompleted =
        mock()

    @Provides
    fun provideObserveAlbumPhotosRemovingProgressUseCase(): ObserveAlbumPhotosRemovingProgress =
        mock()

    @Provides
    fun provideUpdateAlbumPhotosRemovingProgressCompletedUseCase(): UpdateAlbumPhotosRemovingProgressCompleted =
        mock()

    @Provides
    fun provideGetPhotosByIdsUseCase(): GetPhotosByIds = mock()

    @Provides
    fun provideExportAlbumsUseCase(): ExportAlbumsUseCase = mock()

    @Provides
    fun provideDisableExportAlbumsUseCase(): DisableExportAlbumsUseCase = mock()

    @Provides
    fun provideGetPublicAlbumUseCase(): GetPublicAlbumUseCase = mock()

    @Provides
    fun provideDownloadPublicAlbumPhotoThumbnailUseCaseUseCase(): DownloadPublicAlbumPhotoThumbnailUseCase =
        mock()

    @Provides
    fun provideDownloadPublicAlbumPhotoPreviewUseCaseUseCase(): DownloadPublicAlbumPhotoPreviewUseCase =
        mock()

    @Provides
    fun provideGetPublicAlbumNodesDataUseCase(): GetPublicAlbumNodesDataUseCase = mock()

    @Provides
    fun provideGetPublicAlbumNodeDataUseCase(): GetPublicAlbumNodeDataUseCase = mock()

    @Provides
    fun provideAddPhotosToAlbumUseCase(): AddPhotosToAlbumUseCase = mock()

    @Provides
    fun provideImportPublicAlbumUseCase(): ImportPublicAlbumUseCase = mock()

    @Provides
    fun provideSaveAlbumToFolderUseCase(): SaveAlbumToFolderUseCase = mock()
}