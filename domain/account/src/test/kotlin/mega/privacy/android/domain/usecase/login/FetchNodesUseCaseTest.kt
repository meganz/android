package mega.privacy.android.domain.usecase.login

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.login.FetchNodesUpdate
import mega.privacy.android.domain.exception.login.FetchNodesBlockedAccount
import mega.privacy.android.domain.exception.login.FetchNodesErrorAccess
import mega.privacy.android.domain.exception.login.FetchNodesUnknownStatus
import mega.privacy.android.domain.repository.security.LoginRepository
import mega.privacy.android.domain.usecase.setting.ResetChatSettingsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetchNodesUseCaseTest {

    private lateinit var underTest: FetchNodesUseCase

    private val loginRepository = mock<LoginRepository>()
    private val resetChatSettingsUseCase = mock<ResetChatSettingsUseCase>()
    private val loginMutex = mock<Mutex>()

    @BeforeEach
    fun setUp() {
        underTest = FetchNodesUseCase(
            loginRepository = loginRepository,
            resetChatSettingsUseCase = resetChatSettingsUseCase,
            loginMutex = loginMutex
        )
    }

    @AfterEach
    fun tearDown() {
        // Resetting the mocks after each test
        reset(
            loginRepository,
            resetChatSettingsUseCase,
            loginMutex,
        )
    }

    @Test
    fun `test that fetch nodes invokes resetChatSettings if throws FetchNodesErrorAccess`() =
        runTest {
            whenever(loginRepository.fetchNodesFlow()).thenReturn(flow {
                throw FetchNodesErrorAccess(
                    mock()
                )
            })

            underTest.invoke().test {
                assertThat(awaitError()).isInstanceOf(FetchNodesErrorAccess::class.java)
            }

            verify(resetChatSettingsUseCase).invoke()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that fetch nodes invokes resetChatSettings if throws FetchNodesUnknownStatus`() =
        runTest {
            val exception = RuntimeException("resetChatSettings failed")
            whenever(loginRepository.fetchNodesFlow()).thenReturn(flow {
                throw FetchNodesUnknownStatus(
                    mock()
                )
            })
            whenever(resetChatSettingsUseCase()).thenThrow(exception)

            underTest.invoke().test {
                assertThat(awaitError()).isInstanceOf(exception::class.java)
            }

            verify(resetChatSettingsUseCase).invoke()
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that fetch nodes never invokes resetChatSettings if throws FetchNodesBlockedAccount`() =
        runTest {
            whenever(loginRepository.fetchNodesFlow()).thenReturn(flow {
                throw FetchNodesBlockedAccount()
            })

            underTest.invoke().test {
                assertThat(awaitError()).isInstanceOf(FetchNodesBlockedAccount::class.java)
                cancelAndIgnoreRemainingEvents()
            }

            verifyNoInteractions(resetChatSettingsUseCase)
            val inOrder = inOrder(loginMutex)
            inOrder.verify(loginMutex).lock()
            inOrder.verify(loginMutex).unlock()
        }

    @Test
    fun `test that fetch nodes success without error`() = runTest {
        val expectedUpdate = FetchNodesUpdate(mock(), mock())
        whenever(loginRepository.fetchNodesFlow()).thenReturn(flowOf(expectedUpdate))

        underTest.invoke().test {
            assertThat(awaitItem()).isEqualTo(expectedUpdate)
            cancelAndIgnoreRemainingEvents()
        }

        val inOrder = inOrder(loginMutex)
        inOrder.verify(loginMutex).lock()
        inOrder.verify(loginMutex).unlock()
    }
}
