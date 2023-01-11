package mega.privacy.android.domain.di

import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.FileRepository
import mega.privacy.android.domain.usecase.GetLocalFileForNode

/**
 * Shared use case module
 *
 * Provides use case is shared with other components
 */
@Module
@DisableInstallInCheck
internal abstract class InternalOpenFileUseCasesModule {

    companion object {
        @Provides
        fun provideGetLocalFileForNode(fileRepository: FileRepository): GetLocalFileForNode =
            GetLocalFileForNode(fileRepository::getLocalFile)
    }
}