package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.app.data.repository.DefaultContactsRepository
import mega.privacy.android.app.data.repository.DefaultFavouritesRepository
import mega.privacy.android.app.data.repository.DefaultLoginRepository
import mega.privacy.android.app.data.repository.DefaultPhotosRepository
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.FavouritesRepository
import mega.privacy.android.domain.repository.LoginRepository
import mega.privacy.android.domain.repository.PhotosRepository
import kotlin.contracts.ExperimentalContracts

/**
 * Repository module
 *
 * Provides all repository implementations
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @ExperimentalContracts
    @Binds
    abstract fun bindAccountRepository(repository: DefaultAccountRepository): AccountRepository

    @Binds
    abstract fun bindFavouritesRepository(repository: DefaultFavouritesRepository): FavouritesRepository

    @Binds
    abstract fun bindContactsRepository(repository: DefaultContactsRepository): ContactsRepository

    @Binds
    abstract fun bindLoginRepository(repository: DefaultLoginRepository): LoginRepository

    @Binds
    abstract fun bindPhotosRepository(repository: DefaultPhotosRepository): PhotosRepository
}
