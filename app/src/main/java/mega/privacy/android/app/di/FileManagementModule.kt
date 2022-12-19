package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import mega.privacy.android.domain.usecase.DownloadBackgroundFile
import mega.privacy.android.domain.repository.FileRepository

/**
 * File management module.
 *
 * Provides use cases used on multiple view models related to file management.
 */
@Module
@InstallIn(ViewModelComponent::class)
class FileManagementModule {

    @Provides
    fun provideDownloadBackgroundFile(fileRepository: FileRepository): DownloadBackgroundFile =
        DownloadBackgroundFile(fileRepository::downloadBackgroundFile)
}