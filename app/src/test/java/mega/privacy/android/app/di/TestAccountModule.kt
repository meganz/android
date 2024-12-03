package mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import mega.privacy.android.data.mapper.ReferralBonusAchievementsMapper
import mega.privacy.android.domain.di.AccountModule
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.IsUserLoggedIn
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.RetryPendingConnectionsUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.ResetAccountInfoUseCase
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
    private val retryPendingConnectionsUseCase = mock<RetryPendingConnectionsUseCase> {
        on { runBlocking { invoke(false) } }.thenReturn(Unit)
    }
    private val isBusinessAccountActive = mock<IsBusinessAccountActive> {
        on { runBlocking { invoke() } }.thenReturn(false)
    }

    private val getAccountAchievements = mock<GetAccountAchievements> {
        on { runBlocking { invoke(mock(), -1) } }.thenReturn(mock())
    }

    private val getAccountAchievementsOverviewUseCase =
        mock<GetAccountAchievementsOverviewUseCase>()

    private val isUserLoggedIn = mock<IsUserLoggedIn>()

    @Provides
    fun bindGetSessionUseCase() = getSessionUseCase

    @Provides
    fun bindRetryPendingConnections() = retryPendingConnectionsUseCase

    @Provides
    fun bindIsBusinessAccountActive() = isBusinessAccountActive

    @Provides
    fun provideGetAccountAchievements() = getAccountAchievements

    @Provides
    fun provideGetAccountAchievementsOverview() = getAccountAchievementsOverviewUseCase

    @Provides
    fun provideMonitorUserUpdate() = mock<MonitorUserUpdates>()

    @Provides
    fun provideIsUserLoggedIn() = isUserLoggedIn

    @Provides
    fun provideSaveAccountCredentialsUseCase() = mock<SaveAccountCredentialsUseCase>()

    @Provides
    fun provideGetAccountCredentialsUseCase() = mock<GetAccountCredentialsUseCase>()

    @Provides
    fun provideQuerySignupLinkUseCase() = mock<QuerySignupLinkUseCase>()

    @Provides
    fun provideResetAccountInfoUseCase() = mock<ResetAccountInfoUseCase>()

    @Provides
    fun provideLocalLogoutAppUseCase() = mock<LocalLogoutAppUseCase>()

    @Provides
    fun providesMonitorAccountDetail() = mock<MonitorAccountDetailUseCase>()

    @Provides
    fun provideReferralBonusAchievementsMapper() = mock<ReferralBonusAchievementsMapper>()
}
