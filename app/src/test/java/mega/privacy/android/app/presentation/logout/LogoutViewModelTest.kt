package mega.privacy.android.app.presentation.logout

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.logout.model.LogoutState
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.domain.usecase.transfer.OngoingTransfersExistUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LogoutViewModelTest {
    private lateinit var underTest: LogoutViewModel
    private val hasOfflineFilesUseCase = mock<HasOfflineFilesUseCase>()
    private val ongoingTransfersExistUseCase = mock<OngoingTransfersExistUseCase>()
    private val logoutUseCase = mock<LogoutUseCase>()

    @BeforeAll
    internal fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    internal fun setUp() {
        Mockito.reset(
            hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase,
            logoutUseCase,
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
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            ongoingTransfersExistUseCase = ongoingTransfersExistUseCase,
        )
    }

    @AfterAll
    internal fun tearDown() {
        Dispatchers.resetMain()
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
}