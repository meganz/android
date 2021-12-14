package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import mega.privacy.android.app.data.repository.DefaultAccountRepository
import mega.privacy.android.app.data.repository.DefaultSettingsRepository
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.domain.repository.SettingsRepository

@Module
@InstallIn(ApplicationComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindSettingsRepository(repository: DefaultSettingsRepository): SettingsRepository

    @Binds
    abstract fun bindAccountRepository(repository: DefaultAccountRepository): AccountRepository
}