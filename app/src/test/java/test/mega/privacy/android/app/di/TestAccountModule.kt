package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import mega.privacy.android.domain.di.AccountModule
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.usecase.CreateContactLink
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetAccountCredentials
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.LocalLogoutApp
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.QuerySignupLink
import mega.privacy.android.domain.usecase.ResetAccountInfo
import mega.privacy.android.domain.usecase.RetryPendingConnections
import mega.privacy.android.domain.usecase.SaveAccountCredentials
import mega.privacy.android.domain.usecase.account.SetSecureFlag
import mega.privacy.android.domain.usecase.account.UpgradeSecurity
import mega.privacy.android.domain.usecase.account.ChangeEmail
import mega.privacy.android.domain.usecase.account.GetLatestTargetPath
import mega.privacy.android.domain.usecase.account.MonitorSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.account.SetLatestTargetPath
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInApp
import mega.privacy.android.domain.usecase.achievements.GetAccountAchievementsOverview
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [AccountModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestAccountModule {

    private val getSession = mock<GetSession> {
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

    private val getAccountAchievementsOverview = mock<GetAccountAchievementsOverview>()

    private val isUserLoggedIn = mock<IsUserLoggedIn>()

    @Provides
    fun bindGetSession() = getSession

    @Provides
    fun bindRetryPendingConnections() = retryPendingConnections

    @Provides
    fun bindIsBusinessAccountActive() = isBusinessAccountActive

    @Provides
    fun provideGetAccountAchievements() = getAccountAchievements

    @Provides
    fun provideGetAccountAchievementsOverview() = getAccountAchievementsOverview

    @Provides
    fun provideGetMyCredentials() = getMyCredentials

    @Provides
    fun provideCreateContactLink() = createContactLink

    @Provides
    fun provideMonitorUserUpdate() = mock<MonitorUserUpdates>()

    @Provides
    fun provideIsUserLoggedIn() = isUserLoggedIn

    @Provides
    fun provideSaveAccountCredentials() = mock<SaveAccountCredentials>()

    @Provides
    fun provideGetAccountCredentials() = mock<GetAccountCredentials>()

    @Provides
    fun provideChangeEmail() = mock<ChangeEmail>()

    @Provides
    fun provideQuerySignupLink() = mock<QuerySignupLink>()

    @Provides
    fun provideResetAccountInfo() = mock<ResetAccountInfo>()

    @Provides
    fun provideLocalLogoutApp() = mock<LocalLogoutApp>()

    @Provides
    fun provideSetLatestTargetPath() = mock<SetLatestTargetPath>()

    @Provides
    fun provideGetLatestTargetPath() = mock<GetLatestTargetPath>()

    @Provides
    fun provideSetSecureFlag() = mock<SetSecureFlag>()

    @Provides
    fun provideUpgradeSecurity() = mock<UpgradeSecurity>()

    @Provides
    fun provideSetSecurityUpgradeInApp() = mock<SetSecurityUpgradeInApp>()

    @Provides
    fun provideMonitorSecurityUpgradeInApp() = mock<MonitorSecurityUpgradeInApp>()
}
