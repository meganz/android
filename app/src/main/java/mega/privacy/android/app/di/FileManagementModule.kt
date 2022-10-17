package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DownloadBackgroundFile

/**
 * File management module.
 *
 * Provides use cases used on multiple view models related to file management.
 */
@Module
@InstallIn(ViewModelComponent::class)
class FileManagementModule {

    @Provides
    fun provideDownloadBackgroundFile(filesRepository: FilesRepository): DownloadBackgroundFile =
        DownloadBackgroundFile(filesRepository::downloadBackgroundFile)
}