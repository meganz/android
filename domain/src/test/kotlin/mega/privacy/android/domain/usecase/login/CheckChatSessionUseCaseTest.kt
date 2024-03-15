package mega.privacy.android.domain.usecase.login

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatInitState
import mega.privacy.android.domain.usecase.GetChatInitStateUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckChatSessionUseCaseTest {
    private lateinit var underTest: CheckChatSessionUseCase

    private val initialiseMegaChatUseCase = mock<InitialiseMegaChatUseCase>()
    private val getSessionUseCase = mock<GetSessionUseCase>()
    private val getChatInitStateUseCase = mock<GetChatInitStateUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = CheckChatSessionUseCase(
            initialiseMegaChatUseCase = initialiseMegaChatUseCase,
            getSessionUseCase = getSessionUseCase,
            getChatInitStateUseCase = getChatInitStateUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(initialiseMegaChatUseCase, getSessionUseCase, getChatInitStateUseCase)
    }

    @Test
    internal fun `test that initialiseMegaChatUseCase is called when state is NOT_DONE`() =
        runTest {
            whenever(getChatInitStateUseCase()).thenReturn(ChatInitState.NOT_DONE)
            whenever(getSessionUseCase()).thenReturn("session")

            underTest()

            verify(initialiseMegaChatUseCase).invoke("session")
        }

    @Test
    internal fun `test that initialiseMegaChatUseCase is called when state is ERROR`() =
        runTest {
            whenever(getChatInitStateUseCase()).thenReturn(ChatInitState.ERROR)
            whenever(getSessionUseCase()).thenReturn("session")

            underTest()

            verify(initialiseMegaChatUseCase).invoke("session")
        }

    @Test
    internal fun `test that exception is thrown when session not found`() = runTest {
        whenever(getChatInitStateUseCase()).thenReturn(ChatInitState.NOT_DONE)
        whenever(getSessionUseCase()).thenReturn(null)

        assertThrows<IllegalStateException> { underTest() }
    }

    @Test
    internal fun `test that initialiseMegaChatUseCase is not called when state is ANONYMOUS`() =
        runTest {
            whenever(getChatInitStateUseCase()).thenReturn(ChatInitState.ANONYMOUS)
            whenever(getSessionUseCase()).thenReturn("session")

            underTest()

            verifyNoInteractions(initialiseMegaChatUseCase)
        }

    @Test
    internal fun `test that initialiseMegaChatUseCase is not called when state is ONLINE`() =
        runTest {
            whenever(getChatInitStateUseCase()).thenReturn(ChatInitState.ONLINE)
            whenever(getSessionUseCase()).thenReturn("session")

            underTest()

            verifyNoInteractions(initialiseMegaChatUseCase)
        }
}