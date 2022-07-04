package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.di.AccountModule
import mega.privacy.android.app.data.model.UserCredentials
import mega.privacy.android.app.domain.usecase.GetSession
import mega.privacy.android.app.domain.usecase.RetryPendingConnections
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

    @Provides
    fun bindGetSession() = getSession

    @Provides
    fun bindRetryPendingConnections() = retryPendingConnections
}