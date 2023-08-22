package mega.privacy.android.app.di.photos

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.DefaultGetNodeListByIds
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.domain.usecase.DefaultFilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.DefaultFilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.DefaultGetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.DefaultObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.DefaultObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.DefaultSetInitialCUPreferences
import mega.privacy.android.domain.usecase.DefaultUpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.DefaultUpdateAlbumPhotosRemovingProgressCompleted
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetDefaultAlbumPhotos
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosAddingProgress
import mega.privacy.android.domain.usecase.ObserveAlbumPhotosRemovingProgress
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosAddingProgressCompleted
import mega.privacy.android.domain.usecase.UpdateAlbumPhotosRemovingProgressCompleted

@Module
@InstallIn(ViewModelComponent::class)
abstract class PhotosUseCases {

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
    abstract fun bindObserveAlbumPhotosAddingProgress(useCase: DefaultObserveAlbumPhotosAddingProgress): ObserveAlbumPhotosAddingProgress

    @Binds
    abstract fun bindUpdateAlbumPhotosAddingProgressCompleted(useCase: DefaultUpdateAlbumPhotosAddingProgressCompleted): UpdateAlbumPhotosAddingProgressCompleted

    @Binds
    abstract fun bindObserveAlbumPhotosRemovingProgress(useCase: DefaultObserveAlbumPhotosRemovingProgress): ObserveAlbumPhotosRemovingProgress

    @Binds
    abstract fun bindUpdateAlbumPhotosRemovingProgressCompleted(useCase: DefaultUpdateAlbumPhotosRemovingProgressCompleted): UpdateAlbumPhotosRemovingProgressCompleted
}
