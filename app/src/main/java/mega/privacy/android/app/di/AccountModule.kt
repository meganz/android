package mega.privacy.android.app.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.DefaultGetAccountAchievements
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
@InstallIn(SingletonComponent::class, ViewModelComponent::class)
abstract class AccountModule {

    /**
     * Binds the Use Case [MonitorUserUpdates] to its implementation [DefaultMonitorUserUpdates]
     */
    @Binds
    abstract fun bindMonitorUserUpdates(implementation: DefaultMonitorUserUpdates): MonitorUserUpdates

    /**
     * Binds the Use Case [GetAccountDetails] to its implementation [DefaultGetAccountDetails]
     */
    @Binds
    abstract fun bindGetAccountDetails(useCase: DefaultGetAccountDetails): GetAccountDetails

    /**
     * Binds the Use Case [GetAccountAchievements] to its implementation [DefaultGetAccountAchievements]
     */
    @Binds
    abstract fun provideGetAccountAchievements(implementation: DefaultGetAccountAchievements): GetAccountAchievements

    companion object {

        /**
         * Provides the Use Case [GetSession]
         */
        @Provides
        fun provideGetSession(accountRepository: AccountRepository): GetSession =
            GetSession(accountRepository::getSession)

        /**
         * Provides the Use Case [RetryPendingConnections]
         */
        @Provides
        fun provideRetryPendingConnections(accountRepository: AccountRepository): RetryPendingConnections =
            RetryPendingConnections(accountRepository::retryPendingConnections)

        /**
         * Provides the Use Case [IsBusinessAccountActive]
         */
        @Provides
        fun provideIsBusinessAccountActive(accountRepository: AccountRepository): IsBusinessAccountActive =
            IsBusinessAccountActive(accountRepository::isBusinessAccountActive)
    }
}