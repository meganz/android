package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.repository.BusinessRepository
import mega.privacy.android.domain.usecase.CreateContactLink
import mega.privacy.android.domain.usecase.DefaultGetAccountAchievements
import mega.privacy.android.domain.usecase.DefaultIsUserLoggedIn
import mega.privacy.android.domain.usecase.DefaultMonitorUserUpdates
import mega.privacy.android.domain.usecase.DeleteContactLink
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.MonitorAccountDetail
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.RetryPendingConnections
import mega.privacy.android.domain.usecase.account.ChangeEmail
import mega.privacy.android.domain.usecase.account.GetLatestTargetPath
import mega.privacy.android.domain.usecase.account.MonitorSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.SetLatestTargetPath
import mega.privacy.android.domain.usecase.account.SetSecureFlag
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.UpgradeSecurity
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
         * Provides the Use Case [RetryPendingConnections]
         */
        @Provides
        fun provideRetryPendingConnections(accountRepository: AccountRepository): RetryPendingConnections =
            RetryPendingConnections(accountRepository::retryPendingConnections)

        /**
         * Provides the Use Case [IsBusinessAccountActive]
         */
        @Provides
        fun provideIsBusinessAccountActive(repository: BusinessRepository): IsBusinessAccountActive =
            IsBusinessAccountActive(repository::isBusinessAccountActive)

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
        fun provideChangeEmail(accountRepository: AccountRepository): ChangeEmail =
            ChangeEmail(accountRepository::changeEmail)

        @Provides
        fun provideSetLatestTargetPath(accountRepository: AccountRepository): SetLatestTargetPath =
            SetLatestTargetPath(accountRepository::setLatestTargetPathPreference)

        @Provides
        fun provideGetLatestTargetPath(accountRepository: AccountRepository): GetLatestTargetPath =
            GetLatestTargetPath(accountRepository::getLatestTargetPathPreference)

        @Provides
        fun provideUpgradeSecurity(accountRepository: AccountRepository): UpgradeSecurity =
            UpgradeSecurity(accountRepository::upgradeSecurity)

        @Provides
        fun provideMonitorSecurityUpgradeInApp(accountRepository: AccountRepository): MonitorSecurityUpgradeInApp =
            MonitorSecurityUpgradeInApp(accountRepository::monitorSecurityUpgrade)

        @Provides
        fun provideSetSecurityUpgradeInApp(accountRepository: AccountRepository): SetSecurityUpgradeInApp =
            SetSecurityUpgradeInApp(accountRepository::setUpgradeSecurity)

        @Provides
        fun provideSetSecureFlag(accountRepository: AccountRepository): SetSecureFlag =
            SetSecureFlag(accountRepository::setSecureFlag)
    }
}
