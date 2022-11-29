package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.AccountModule
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.GetSession
import mega.privacy.android.domain.usecase.IsBusinessAccountActive
import mega.privacy.android.domain.usecase.RetryPendingConnections
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [AccountModule::class],
    components = [SingletonComponent::class, ViewModelComponent::class]
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

    @Provides
    fun bindGetSession() = getSession

    @Provides
    fun bindRetryPendingConnections() = retryPendingConnections

    @Provides
    fun bindIsBusinessAccountActive() = isBusinessAccountActive

    @Provides
    fun provideGetAccountAchievements() = getAccountAchievements

    @Provides
    fun provideGetMyCredentials() = getMyCredentials
}