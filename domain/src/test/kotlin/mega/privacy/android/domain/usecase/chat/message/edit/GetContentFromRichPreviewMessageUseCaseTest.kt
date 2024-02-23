package mega.privacy.android.domain.usecase.chat.message.edit

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContentFromRichPreviewMessageUseCaseTest {

    private lateinit var underTest: GetContentFromRichPreviewMessageUseCase

    @BeforeEach
    fun setup() {
        underTest = GetContentFromRichPreviewMessageUseCase()
    }

    @Test
    fun `test that get content from rich preview message use case returns correctly`() = runTest {
        val content = "content"
        val message = mock<RichPreviewMessage> {
            on { this.content }.thenReturn(content)
        }
        Truth.assertThat(underTest.invoke(message)).isEqualTo(content)
    }

    @Test
    fun `test that get content from non rich preview message use case returns correctly`() =
        runTest {
            val message = mock<ContactAttachmentMessage>()
            Truth.assertThat(underTest.invoke(message)).isNull()
        }
}