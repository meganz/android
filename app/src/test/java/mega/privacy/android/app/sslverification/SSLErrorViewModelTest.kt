package mega.privacy.android.app.sslverification

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.sslverification.model.SSLDialogState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.apiserver.ResetConnectionUseCase
import mega.privacy.android.domain.usecase.domainmigration.GetDomainNameUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SSLErrorViewModelTest {
    private lateinit var underTest: SSLErrorViewModel

    private val getDomainNameUseCase = mock<GetDomainNameUseCase>()
    private val resetConnectionUseCase = mock<ResetConnectionUseCase>()

    fun initUnderTest(scope: CoroutineScope) {
        underTest = SSLErrorViewModel(
            getDomainNameUseCase = getDomainNameUseCase,
            resetConnectionUseCase = resetConnectionUseCase,
            applicationScope = scope,
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        initUnderTest(this)
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(SSLDialogState.Loading)
        }
    }

    @Test
    fun `test that ready state is returned if domain is returned`() = runTest {
        initUnderTest(this)
        val domainName = "https://example.com"
        getDomainNameUseCase.stub {
            onBlocking { invoke() }.thenReturn(domainName)
        }
        underTest.state.test {
            val actual = awaitItem()
            assertThat(actual).isInstanceOf(SSLDialogState.Ready::class.java)
            assertThat((actual as SSLDialogState.Ready).webUrl).isEqualTo(domainName)
        }
    }

    @Test
    fun `test that calling onRetry calls reset connections with disable pinning false`() = runTest {
        initUnderTest(this)
        underTest.onRetry()
        advanceUntilIdle()
        verify(resetConnectionUseCase)(disablePinning = false)
    }

    @Test
    fun `test that calling onDismiss calls reset connections with disable pinning true`() =
        runTest {
            initUnderTest(this)
            underTest.onDismiss()
            advanceUntilIdle()
            verify(resetConnectionUseCase)(disablePinning = true)
        }
}