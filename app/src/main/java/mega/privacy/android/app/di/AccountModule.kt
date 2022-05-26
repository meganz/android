package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.usecase.GetCredentials
import mega.privacy.android.app.domain.usecase.DefaultGetCredentials

/**
 * Account module.
 *
 * Provides all account implementations.
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    abstract fun bindCredentialsExists(useCase: DefaultGetCredentials): GetCredentials
}