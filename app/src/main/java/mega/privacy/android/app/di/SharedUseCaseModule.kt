package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.CheckAccessErrorExtended
import mega.privacy.android.data.repository.FilesRepository

/**
 * Shared use case module
 *
 * Provides use case is shared with other components
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SharedUseCaseModule {

    companion object {
        /**
         * Provide check access error extended
         */
        @Provides
        fun provideCheckAccessErrorExtended(repository: FilesRepository) =
            CheckAccessErrorExtended(repository::checkAccessErrorExtended)
    }
}