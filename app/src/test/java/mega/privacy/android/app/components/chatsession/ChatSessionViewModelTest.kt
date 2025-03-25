package mega.privacy.android.app.components.chatsession

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.login.CheckChatSessionUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
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

}