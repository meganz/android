package mega.privacy.android.app.di.chat

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.repository.GalleryFilesRepository
import mega.privacy.android.domain.usecase.GetAllGalleryImages
import mega.privacy.android.domain.usecase.GetAllGalleryVideos

/**
 * Provide implementation for use cases that are regarding gallery files.
 */
@Module
@InstallIn(ViewModelComponent::class)
class GalleryFilesUseCases {

    /**
     * Provide GetAllGalleryImages implementation
     */
    @Provides
    fun provideGetAllGalleryImages(repository: GalleryFilesRepository): GetAllGalleryImages =
        GetAllGalleryImages(repository::getAllGalleryImages)

    /**
     * Provide GetAllGalleryVideos implementation
     */
    @Provides
    fun provideGetAllGalleryVideos(repository: GalleryFilesRepository): GetAllGalleryVideos =
        GetAllGalleryVideos(repository::getAllGalleryVideos)
}