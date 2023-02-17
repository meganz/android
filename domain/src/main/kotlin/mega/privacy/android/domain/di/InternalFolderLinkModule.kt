package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.FolderLinkRepository
import mega.privacy.android.domain.usecase.folderlink.LoginToFolder

/**
 * Internal module class to provide all Use Cases for [FolderLinkModule]
 */
@Module
@DisableInstallInCheck
internal abstract class InternalFolderLinkModule {

    companion object {
        @Provides
        fun provideLoginToFolder(folderLinkRepository: FolderLinkRepository): LoginToFolder =
            LoginToFolder(folderLinkRepository::loginToFolder)
    }
}