package mega.privacy.android.app.components.chatsession

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
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
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ChatSessionViewModelTest {
    private lateinit var underTest: ChatSessionViewModel

    private val checkChatSessionUseCase: CheckChatSessionUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = ChatSessionViewModel(
            checkChatSessionUseCase = checkChatSessionUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            checkChatSessionUseCase,
        )
    }

    @Test
    fun `test that checkChatSession should call checkChatSessionUseCase`() =
        runTest {
            underTest.checkChatSession()

            verify(checkChatSessionUseCase).invoke()
        }


    @Test
    fun `test that successful checkChatSession updates isChatSessionValid to true`() = runTest {
        underTest.checkChatSession()
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(ChatSessionState.Valid)
        }
    }

    @Test
    fun `test that failed checkChatSession updates isChatSessionValid to false`() = runTest {
        whenever(checkChatSessionUseCase()).thenAnswer {
            throw Exception("Call failed")
        }

        underTest.checkChatSession()

        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(ChatSessionState.Invalid)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that optimistic parameter sets initial state correctly`(
        optimistic: Boolean,
    ) = runTest {
        Dispatchers.setMain(StandardTestDispatcher()) //we need async execution for this test
        setUp() // ensure initial status

        whenever(checkChatSessionUseCase()).thenAnswer {
            throw Exception("Call failed")
        }

        assertThat(underTest.state.value).isEqualTo(ChatSessionState.Pending)
        underTest.checkChatSession(optimistic)

        underTest.state.test {
            if (optimistic) {
                assertThat(awaitItem()).isEqualTo(ChatSessionState.Valid)
            } else {
                assertThat(awaitItem()).isEqualTo(ChatSessionState.Pending)
            }
            assertThat(awaitItem()).isEqualTo(ChatSessionState.Invalid)
        }

        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

}