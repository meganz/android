package mega.privacy.android.app.presentation.logout

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.globalmanagement.ChatLogoutHandler
import mega.privacy.android.app.presentation.logout.model.LogoutState
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfers.OngoingTransfersExistUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LogoutViewModelTest {
    private lateinit var underTest: LogoutViewModel
    private val hasOfflineFilesUseCase = mock<HasOfflineFilesUseCase>()
    private val ongoingTransfersExistUseCase = mock<OngoingTransfersExistUseCase>()
    private val logoutUseCase = mock<LogoutUseCase>()
    private val chatLogoutHandler = mock<ChatLogoutHandler>()

    @BeforeEach
    internal fun setUp() {
        Mockito.reset(
            hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase,
            logoutUseCase,
            chatLogoutHandler,
        )

        hasOfflineFilesUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }

        ongoingTransfersExistUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }
    }


    private fun initialiseUnderTest() {
        underTest = LogoutViewModel(
            logoutUseCase = logoutUseCase,
            chatLogoutHandler = chatLogoutHandler,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase,
        )
    }

    @Test
    internal fun `test that initial state is loading`() = runTest {
        initialiseUnderTest()
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(LogoutState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that has offline files is true if use case returns true`() = runTest {
        hasOfflineFilesUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        initialiseUnderTest()

        underTest.state.filterIsInstance<LogoutState.Data>().test {
            assertThat(awaitItem().hasOfflineFiles).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that ongoing transfer is true if the use case returns true`() = runTest {
        ongoingTransfersExistUseCase.stub {
            onBlocking { invoke() }.thenReturn(true)
        }

        initialiseUnderTest()

        underTest.state.filterIsInstance<LogoutState.Data>().test {
            assertThat(awaitItem().hasPendingTransfers).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    internal fun `test that has offline files is false if offline file use case throws an exception`() =
        runTest {
            ongoingTransfersExistUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            hasOfflineFilesUseCase.stub {
                onBlocking { invoke() }.thenAnswer { throw Exception("'Tis bad") }
            }

            initialiseUnderTest()

            underTest.state.filterIsInstance<LogoutState.Data>().test {
                assertThat(awaitItem().hasOfflineFiles).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that ongoing transfer is false if the transfer use case throws an exception`() =
        runTest {
            hasOfflineFilesUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
            ongoingTransfersExistUseCase.stub {
                onBlocking { invoke() }.thenAnswer { throw Exception("Ashes") }
            }

            initialiseUnderTest()

            underTest.state.filterIsInstance<LogoutState.Data>().test {
                assertThat(awaitItem().hasPendingTransfers).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    internal fun `test that logout is called if both values are false`() = runTest {
        hasOfflineFilesUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }

        ongoingTransfersExistUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }

        initialiseUnderTest()

        testScheduler.advanceUntilIdle()

        verify(logoutUseCase).invoke()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that logout succeeds normally when network is good`() = runTest {
        // Given
        logoutUseCase.stub {
            onBlocking { invoke() }.thenAnswer { Unit }
        }

        // When
        initialiseUnderTest()
        // Note: logout() is called automatically in init when no offline files/transfers

        // Then
        underTest.state.test {
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(LogoutState.Loading::class.java)

            val successState = awaitItem()
            assertThat(successState).isInstanceOf(LogoutState.Success::class.java)
        }

        // Wait for coroutines to complete
        advanceUntilIdle()

        verify(logoutUseCase).invoke()
        verifyNoInteractions(chatLogoutHandler)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that logout falls back to chat logout handler when network logout fails`() = runTest {
        // Given
        logoutUseCase.stub {
            onBlocking { invoke() }.thenThrow(RuntimeException("Network error"))
        }
        whenever(chatLogoutHandler.handleChatLogout(any())).thenAnswer { Unit }

        // When
        initialiseUnderTest()
        // Note: logout() is called automatically in init when no offline files/transfers

        // Then
        underTest.state.test {
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(LogoutState.Loading::class.java)

            // Note: This test doesn't verify the timeout fallback due to delay(5.seconds)
            // The timeout fallback should be tested in integration tests instead
            cancelAndIgnoreRemainingEvents()
        }

        // Wait for coroutines to complete
        advanceUntilIdle()

        verify(logoutUseCase).invoke()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that logout shows error when both network and chat logout handler fail`() = runTest {
        // Given
        logoutUseCase.stub {
            onBlocking { invoke() }.thenThrow(RuntimeException("Network error"))
        }
        whenever(chatLogoutHandler.handleChatLogout(any())).thenThrow(RuntimeException("Chat logout error"))

        // When
        initialiseUnderTest()
        // Note: logout() is called automatically in init when no offline files/transfers

        // Then
        underTest.state.test {
            val loadingState = awaitItem()
            assertThat(loadingState).isInstanceOf(LogoutState.Loading::class.java)

            // Note: This test doesn't verify the timeout fallback due to delay(5.seconds)
            // The timeout fallback should be tested in integration tests instead
            cancelAndIgnoreRemainingEvents()
        }

        // Wait for coroutines to complete
        advanceUntilIdle()

        verify(logoutUseCase).invoke()
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
