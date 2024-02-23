package mega.privacy.android.domain.usecase.chat.message.edit

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMessageContentUseCaseTest {

    private lateinit var underTest: GetMessageContentUseCase

    private val getContentFromNormalMessageUseCase = mock<GetContentFromNormalMessageUseCase>()
    private val getContentFromRichPreviewMessageUseCase =
        mock<GetContentFromRichPreviewMessageUseCase>()

    private val getContentFromMessagesUseCases = setOf(
        getContentFromNormalMessageUseCase,
        getContentFromRichPreviewMessageUseCase,
    )

    @BeforeAll
    fun setup() {
        underTest = GetMessageContentUseCase(getContentFromMessagesUseCases)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getContentFromNormalMessageUseCase,
            getContentFromRichPreviewMessageUseCase,
        )
    }

    @Test
    fun `test that get message content from a normal message returns correctly`() = runTest {
        val content = "content"
        val message = mock<NormalMessage> {
            on { this.content } doReturn content
        }
        whenever(getContentFromNormalMessageUseCase(message)).thenReturn(content)
        Truth.assertThat(underTest.invoke(message)).isEqualTo(content)
    }

    @Test
    fun `test that get message content from a rich preview message returns correctly`() = runTest {
        val content = "content"
        val message = mock<RichPreviewMessage> {
            on { this.content } doReturn content
        }
        whenever(getContentFromRichPreviewMessageUseCase(message)).thenReturn(content)
        Truth.assertThat(underTest.invoke(message)).isEqualTo(content)
    }

    @Test
    fun `test that get message content from a contact message returns correctly`() = runTest {
        val message = mock<ContactAttachmentMessage>()
        Truth.assertThat(underTest.invoke(message)).isEqualTo("")
    }
}