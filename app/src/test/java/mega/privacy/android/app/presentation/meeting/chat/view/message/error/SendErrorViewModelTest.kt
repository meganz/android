package mega.privacy.android.app.presentation.meeting.chat.view.message.error

import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.usecase.chat.message.ResendMessageUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking

@ExtendWith(CoroutineMainDispatcherExtension::class)
class SendErrorViewModelTest {
    private lateinit var underTest: SendErrorViewModel

    private val resendMessageUseCase = mock<ResendMessageUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = SendErrorViewModel(resendMessageUseCase = resendMessageUseCase)
    }

    @Test
    fun `test that resend is called when attempting to retry a message send`() {
        val message = mock<TypedMessage>()
        underTest.retry(setOf(message))
        verifyBlocking(resendMessageUseCase) { this(message) }
    }

    @Test
    fun `test that failures do not prevent subsequent messages being retried`() {
        val failedMessage = mock<TypedMessage>()
        val message = mock<TypedMessage>()
        resendMessageUseCase.stub {
            onBlocking { invoke(failedMessage) }.thenAnswer { throw Throwable() }
            onBlocking { invoke(message) }.thenAnswer {}
        }

        val input = setOf(failedMessage, message)
        underTest.retry(input)

        verifyBlocking(resendMessageUseCase, times(input.size)) { this(any()) }
    }
}