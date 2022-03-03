package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.data.repository.*
import mega.privacy.android.app.domain.repository.*
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.app.data.repository.DefaultSettingsRepository
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.domain.repository.SettingsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindAccountRepository(repository: DefaultAccountRepository): AccountRepository

    @Binds
    abstract fun bindNetworkRepository(repository: DefaultNetworkRepository): NetworkRepository

    @Binds
    abstract fun bindFilesRepository(implementation: MegaFilesRepository): FilesRepository
}