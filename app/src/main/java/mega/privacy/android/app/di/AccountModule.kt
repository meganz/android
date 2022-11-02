package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.DefaultGetAccountDetails
import mega.privacy.android.domain.usecase.DefaultMonitorUserUpdates
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.RetryPendingConnections

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
    abstract fun bindMonitorUserUpdates(implementation: DefaultMonitorUserUpdates): MonitorUserUpdates

    @Binds
    abstract fun bindGetAccountDetails(useCase: DefaultGetAccountDetails): GetAccountDetails

    companion object {
        @Provides
        fun provideGetSession(accountRepository: AccountRepository): GetSession =
            GetSession(accountRepository::getSession)

        @Provides
        fun provideRetryPendingConnections(accountRepository: AccountRepository): RetryPendingConnections =
            RetryPendingConnections(accountRepository::retryPendingConnections)

        @Provides
        fun provideIsBusinessAccountActive(accountRepository: AccountRepository): IsBusinessAccountActive =
            IsBusinessAccountActive(accountRepository::isBusinessAccountActive)

        @Provides
        fun provideGetAccountAchievements(accountRepository: AccountRepository): GetAccountAchievements =
            GetAccountAchievements(accountRepository::getAccountAchievements)
    }
}