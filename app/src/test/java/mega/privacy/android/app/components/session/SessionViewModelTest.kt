package mega.privacy.android.app.components.session

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.chat.RetryConnectionsAndSignalPresenceUseCase
import mega.privacy.android.domain.usecase.login.CheckChatSessionUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SessionViewModelTest {
    private lateinit var underTest: SessionViewModel

    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()
    private val checkChatSessionUseCase: CheckChatSessionUseCase = mock()
    private val retryConnectionsAndSignalPresenceUseCase: RetryConnectionsAndSignalPresenceUseCase =
        mock()

    @BeforeAll
    fun setUp() {
        underTest = SessionViewModel(
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            checkChatSessionUseCase = checkChatSessionUseCase,
            retryConnectionsAndSignalPresenceUseCase = retryConnectionsAndSignalPresenceUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            rootNodeExistsUseCase,
            checkChatSessionUseCase,
            retryConnectionsAndSignalPresenceUseCase
        )
    }

    @Test
    fun `test that checkSdkSession should call checkChatSessionUseCase when shouldCheckChatSession is true`() =
        runTest {
            whenever(checkChatSessionUseCase()).thenReturn(Unit)

            underTest.checkSdkSession(true)

            verify(checkChatSessionUseCase).invoke()
        }

    @Test
    fun `test that checkSdkSession should not call checkChatSessionUseCase when shouldCheckChatSession is false`() =
        runTest {
            underTest.checkSdkSession(false)

            verifyNoInteractions(checkChatSessionUseCase)
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that checkSdkSession updates state correctly`(
        shouldCheckChatSession: Boolean,
    ) = runTest {
        whenever(rootNodeExistsUseCase()).thenReturn(true)

        underTest.checkSdkSession(shouldCheckChatSession)
        underTest.state.test {
            assertThat(awaitItem().isRootNodeExists).isTrue()
        }
    }

    @Test
    fun `test that check chat sdk session update state correctly`() = runTest {
        whenever(checkChatSessionUseCase()).thenReturn(Unit)

        underTest.checkSdkSession(true)

        underTest.state.test {
            assertThat(awaitItem().isChatSessionValid).isTrue()
        }
    }

    @Test
    fun `test that retry connections and signal presence is called`() = runTest {
        underTest.retryConnectionsAndSignalPresence()

        verify(retryConnectionsAndSignalPresenceUseCase).invoke()
    }
}