package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.normal.ContactLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.usecase.link.ExtractContactLinkUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateNormalChatMessageUseCaseTest {
    private lateinit var underTest: CreateNormalChatMessageUseCase
    private val extractContactLinkUseCase: ExtractContactLinkUseCase = mock()

    @BeforeEach
    internal fun setUp() {
        underTest = CreateNormalChatMessageUseCase(extractContactLinkUseCase)
    }

    @AfterEach
    internal fun tearDown() {
        reset(extractContactLinkUseCase)
    }

    @Test
    fun `test that normal message is returned`() {
        whenever(extractContactLinkUseCase(any())).thenReturn(null)
        val message = mock(ChatMessage::class.java)
        Truth.assertThat(underTest.invoke(CreateTypedMessageRequest(message, true)))
            .isInstanceOf(TextMessage::class.java)
    }

    @Test
    fun `test that contact link message is returned`() {
        whenever(extractContactLinkUseCase(any())).thenReturn("contactLink")
        val message = mock(ChatMessage::class.java)
        Truth.assertThat(underTest.invoke(CreateTypedMessageRequest(message, true)))
            .isInstanceOf(ContactLinkMessage::class.java)
    }
}