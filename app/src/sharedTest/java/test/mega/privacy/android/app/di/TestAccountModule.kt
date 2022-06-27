package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.UserCredentials
import mega.privacy.android.app.di.AccountModule
import mega.privacy.android.app.domain.usecase.GetCredentials
import mega.privacy.android.app.domain.usecase.RetryPendingConnections
import org.mockito.kotlin.mock

@TestInstallIn(
    replaces = [AccountModule::class],
    components = [SingletonComponent::class]
)
@Module
object TestAccountModule {

    private val getCredentials = mock<GetCredentials> {
        on { runBlocking { invoke() } }.thenReturn(UserCredentials(""))
    }
    private val retryPendingConnections = mock<RetryPendingConnections> {
        on { runBlocking { invoke(false) } }
    }

    @Provides
    fun bindGetCredentials() = getCredentials

    @Provides
    fun bindRetryPendingConnections() = retryPendingConnections
}