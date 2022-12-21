package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.DefaultPhotosRepository
import mega.privacy.android.domain.repository.PhotosRepository

/**
 * Repository module
 *
 * Provides all repository implementations
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindPhotosRepository(repository: DefaultPhotosRepository): PhotosRepository
}
