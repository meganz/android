package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import mega.privacy.android.data.mapper.ReferralBonusAchievementsMapper
import mega.privacy.android.domain.di.AccountModule
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.usecase.CreateContactLink
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.MonitorAccountDetail
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.RetryPendingConnections
import mega.privacy.android.domain.usecase.account.ChangeEmail
import mega.privacy.android.domain.usecase.account.MonitorSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.ResetAccountInfoUseCase
import mega.privacy.android.domain.usecase.account.SetSecureFlag
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.UpgradeSecurity
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverviewUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.login.GetSessionUseCase
import mega.privacy.android.domain.usecase.login.LocalLogoutAppUseCase
import mega.privacy.android.domain.usecase.login.QuerySignupLinkUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [AccountModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestAccountModule {

    private val getSessionUseCase = mock<GetSessionUseCase> {
        on { runBlocking { invoke() } }.thenReturn("")
    }
    private val retryPendingConnections = mock<RetryPendingConnections> {
        on { runBlocking { invoke(false) } }
    }
    private val isBusinessAccountActive = mock<IsBusinessAccountActive> {
        on { runBlocking { invoke() } }.thenReturn(false)
    }

    private val getAccountAchievements = mock<GetAccountAchievements> {
        on { runBlocking { invoke(mock(), -1) } }.thenReturn(mock())
    }

    private val getMyCredentials = mock<GetMyCredentials> {
        on { runBlocking { invoke() } }.thenReturn(mock<AccountCredentials.MyAccountCredentials>())
    }


    private val createContactLink = mock<CreateContactLink>()

    private val getAccountAchievementsOverviewUseCase =
        mock<GetAccountAchievementsOverviewUseCase>()

    private val isUserLoggedIn = mock<IsUserLoggedIn>()

    @Provides
    fun bindGetSessionUseCase() = getSessionUseCase

    @Provides
    fun bindRetryPendingConnections() = retryPendingConnections

    @Provides
    fun bindIsBusinessAccountActive() = isBusinessAccountActive

    @Provides
    fun provideGetAccountAchievements() = getAccountAchievements

    @Provides
    fun provideGetAccountAchievementsOverview() = getAccountAchievementsOverviewUseCase

    @Provides
    fun provideGetMyCredentials() = getMyCredentials

    @Provides
    fun provideCreateContactLink() = createContactLink

    @Provides
    fun provideMonitorUserUpdate() = mock<MonitorUserUpdates>()

    @Provides
    fun provideIsUserLoggedIn() = isUserLoggedIn

    @Provides
    fun provideSaveAccountCredentialsUseCase() = mock<SaveAccountCredentialsUseCase>()

    @Provides
    fun provideGetAccountCredentialsUseCase() = mock<GetAccountCredentialsUseCase>()

    @Provides
    fun provideChangeEmail() = mock<ChangeEmail>()

    @Provides
    fun provideQuerySignupLinkUseCase() = mock<QuerySignupLinkUseCase>()

    @Provides
    fun provideResetAccountInfoUseCase() = mock<ResetAccountInfoUseCase>()

    @Provides
    fun provideLocalLogoutAppUseCase() = mock<LocalLogoutAppUseCase>()

    @Provides
    fun provideSetSecureFlag() = mock<SetSecureFlag>()

    @Provides
    fun provideUpgradeSecurity() = mock<UpgradeSecurity>()

    @Provides
    fun provideSetSecurityUpgradeInApp() = mock<SetSecurityUpgradeInApp>()

    @Provides
    fun provideMonitorSecurityUpgradeInApp() = mock<MonitorSecurityUpgradeInApp>()

    @Provides
    fun providesMonitorAccountDetail() = mock<MonitorAccountDetail>()

    @Provides
    fun provideReferralBonusAchievementsMapper() = mock<ReferralBonusAchievementsMapper>()
}
