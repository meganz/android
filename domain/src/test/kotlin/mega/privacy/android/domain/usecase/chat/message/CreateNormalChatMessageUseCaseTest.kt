package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.LinkDetail
import mega.privacy.android.domain.entity.chat.message.request.CreateTypedMessageRequest
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.usecase.chat.GetLinkTypesUseCase
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
    private val getLinkTypesUseCase: GetLinkTypesUseCase = mock()

    @BeforeEach
    internal fun setUp() {
        underTest = CreateNormalChatMessageUseCase(getLinkTypesUseCase)
    }

    @AfterEach
    internal fun tearDown() {
        reset(getLinkTypesUseCase)
    }

    @Test
    fun `test that normal message is returned`() {
        whenever(getLinkTypesUseCase(any())).thenReturn(emptyList())
        val message = mock(ChatMessage::class.java)
        Truth.assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    message = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
                )
            )
        )
            .isInstanceOf(TextMessage::class.java)
    }

    @Test
    fun `test that contact link message is returned`() {
        whenever(getLinkTypesUseCase(any())).thenReturn(
            listOf(
                LinkDetail(
                    "link",
                    RegexPatternType.CONTACT_LINK
                )
            )
        )
        val message = mock(ChatMessage::class.java)
        Truth.assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    message = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
                )
            )
        )
            .isInstanceOf(TextLinkMessage::class.java)
    }
}