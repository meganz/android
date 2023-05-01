package mega.privacy.android.app.di.photos

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultGetNodeListByIds
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.domain.repository.ImageRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.repository.SettingsRepository
import mega.privacy.android.domain.usecase.DefaultFilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.DefaultFilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.DefaultGetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.DefaultGetPreview
import mega.privacy.android.domain.usecase.DefaultGetThumbnail
import mega.privacy.android.domain.usecase.DefaultGetTypedNodesFromFolder
import mega.privacy.android.domain.usecase.DefaultObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.DefaultObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.DefaultSetInitialCUPreferences
import mega.privacy.android.domain.usecase.DefaultUpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.DefaultUpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.DownloadPreview
import mega.privacy.android.domain.usecase.DownloadThumbnail
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.GetPhotosByIds
import mega.privacy.android.domain.usecase.GetPreview
import mega.privacy.android.domain.usecase.GetThumbnail
import mega.privacy.android.domain.usecase.GetTypedNodesFromFolder
import mega.privacy.android.domain.usecase.IsCameraSyncPreferenceEnabled
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted

@Module
@InstallIn(ViewModelComponent::class)
abstract class PhotosUseCases {

    @Binds
    abstract fun bindGetThumbnail(useCase: DefaultGetThumbnail): GetThumbnail

    @Binds
    abstract fun bindGetPreview(useCase: DefaultGetPreview): GetPreview

    @Binds
    abstract fun bindFilterCameraUploadPhotos(useCase: DefaultFilterCameraUploadPhotos): FilterCameraUploadPhotos

    @Binds
    abstract fun bindFilterCloudDrivePhotos(useCase: DefaultFilterCloudDrivePhotos): FilterCloudDrivePhotos

    @Binds
    abstract fun bindSetInitialCUPreferences(useCase: DefaultSetInitialCUPreferences): SetInitialCUPreferences

    @Binds
    abstract fun bindGetNodeListByIds(useCase: DefaultGetNodeListByIds): GetNodeListByIds

    @Binds
    abstract fun bindGetDefaultAlbumPhotos(useCase: DefaultGetDefaultAlbumPhotos): GetDefaultAlbumPhotos

    @Binds
    abstract fun bindGetTypedNodesFromFolder(useCase: DefaultGetTypedNodesFromFolder): GetTypedNodesFromFolder

    @Binds
    abstract fun bindObserveAlbumPhotosAddingProgress(useCase: DefaultObserveAlbumPhotosAddingProgress): ObserveAlbumPhotosAddingProgress

    @Binds
    abstract fun bindUpdateAlbumPhotosAddingProgressCompleted(useCase: DefaultUpdateAlbumPhotosAddingProgressCompleted): UpdateAlbumPhotosAddingProgressCompleted

    @Binds
    abstract fun bindObserveAlbumPhotosRemovingProgress(useCase: DefaultObserveAlbumPhotosRemovingProgress): ObserveAlbumPhotosRemovingProgress

    @Binds
    abstract fun bindUpdateAlbumPhotosRemovingProgressCompleted(useCase: DefaultUpdateAlbumPhotosRemovingProgressCompleted): UpdateAlbumPhotosRemovingProgressCompleted

    companion object {
        @Provides
        fun providesIsCameraSyncPreferenceEnabled(settingsRepository: SettingsRepository): IsCameraSyncPreferenceEnabled =
            IsCameraSyncPreferenceEnabled(settingsRepository::isCameraSyncPreferenceEnabled)

        @Provides
        fun provideDownloadThumbnail(repository: ImageRepository): DownloadThumbnail =
            DownloadThumbnail(repository::downloadThumbnail)

        @Provides
        fun provideDownloadPreview(repository: ImageRepository): DownloadPreview =
            DownloadPreview(repository::downloadPreview)

        @Provides
        fun provideGetPhotosByIds(repository: PhotosRepository): GetPhotosByIds =
            GetPhotosByIds(repository::getPhotosByIds)
    }
}