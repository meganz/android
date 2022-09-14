package mega.privacy.android.app.di.chat

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.app.data.repository.DefaultGalleryFilesRepository
import mega.privacy.android.domain.usecase.GetAllGalleryFiles

/**
 * Provide implementation for use cases that are regarding gallery files.
 */
@Module
@InstallIn(ViewModelComponent::class)
class GalleryFilesUseCases {

    /**
     * Provide GetAllGalleryFiles implementation
     */
    @Provides
    fun provideGetAllGalleryFiles(repository: DefaultGalleryFilesRepository): GetAllGalleryFiles =
        GetAllGalleryFiles(repository::getAllGalleryFiles)
}