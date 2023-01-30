package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.CreateContactLink
import mega.privacy.android.domain.usecase.DefaultGetAccountAchievements
import mega.privacy.android.domain.usecase.DefaultGetAccountDetails
import mega.privacy.android.domain.usecase.DefaultIsUserLoggedIn
import mega.privacy.android.domain.usecase.DefaultMonitorUserUpdates
import mega.privacy.android.domain.usecase.DeleteContactLink
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetAccountDetails
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.MonitorAccountDetail
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.RetryPendingConnections
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverview

/**
 * Account module.
 *
 * Provides all account implementations.
 *
 */
@Module
@DisableInstallInCheck
internal abstract class InternalAccountModule {

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

    /**
     * Binds the Use Case [IsUserLoggedIn] to its implementation [DefaultIsUserLoggedIn]
     */
    @Binds
    abstract fun bindIsUserLoggedIn(useCase: DefaultIsUserLoggedIn): IsUserLoggedIn


    companion object {

        @Provides
        fun provideAchievementsDetailUseCase(accountRepository: AccountRepository): GetAccountAchievementsOverview =
            GetAccountAchievementsOverview(accountRepository::getAccountAchievementsOverview)

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

        /**
         * Provides the Use Case [GetMyCredentials]
         */
        @Provides
        fun provideGetMyCredentials(accountRepository: AccountRepository): GetMyCredentials =
            GetMyCredentials(accountRepository::getMyCredentials)

        /**
         * Provides the use case [CreateContactLink]
         */
        @Provides
        fun provideCreateContactLink(accountRepository: AccountRepository): CreateContactLink =
            CreateContactLink(accountRepository::createContactLink)

        /**
         * Provides the use case [DeleteContactLink]
         */
        @Provides
        fun provideDeleteContactLink(accountRepository: AccountRepository): DeleteContactLink =
            DeleteContactLink(accountRepository::deleteContactLink)

        @Provides
        fun provideMonitorAccountDetail(accountRepository: AccountRepository): MonitorAccountDetail =
            MonitorAccountDetail(accountRepository::monitorAccountDetail)

        @Provides
        fun provideSaveAccountCredentials(accountRepository: AccountRepository): SaveAccountCredentials =
            SaveAccountCredentials(accountRepository::saveAccountCredentials)
    }
}