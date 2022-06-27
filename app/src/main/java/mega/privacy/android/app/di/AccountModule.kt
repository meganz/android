package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.domain.usecase.GetCredentials
import mega.privacy.android.app.domain.usecase.RetryPendingConnections

/**
 * Account module.
 *
 * Provides all account implementations.
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    companion object {
        @Provides
        fun provideGetCredentials(accountRepository: AccountRepository): GetCredentials =
            GetCredentials(accountRepository::getCredentials)

        @Provides
        fun provideRetryPendingConnections(accountRepository: AccountRepository): RetryPendingConnections =
            RetryPendingConnections(accountRepository::retryPendingConnections)
    }
}