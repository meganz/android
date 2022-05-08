package mega.privacy.android.app.di.photos

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.domain.usecase.*

@Module
@InstallIn(ViewModelComponent::class)
abstract class PhotosUseCases {

    @Binds
    abstract fun bindGetCameraUploadFolder(useCase: DefaultGetCameraUploadFolder): GetCameraUploadFolder

    @Binds
    abstract fun bindGetMediaUploadFolder(useCase: DefaultGetMediaUploadFolder): GetMediaUploadFolder

    @Binds
    abstract fun bindGetThumbnailFromServer(useCase: DefaultGetThumbnail): GetThumbnail
}