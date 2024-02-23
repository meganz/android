package mega.privacy.android.domain.usecase.chat.message.edit

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetContentFromNormalMessageUseCaseTest {

    private lateinit var underTest: GetContentFromNormalMessageUseCase

    @BeforeEach
    fun setup() {
        underTest = GetContentFromNormalMessageUseCase()
    }

    @Test
    fun `test that get content from normal message use case returns correctly`() = runTest {
        val content = "content"
        val message = mock<NormalMessage> {
            on { this.content }.thenReturn(content)
        }
        Truth.assertThat(underTest.invoke(message)).isEqualTo(content)
    }

    @Test
    fun `test that get content from non normal message use case returns correctly`() = runTest {
        val message = mock<ContactAttachmentMessage>()
        Truth.assertThat(underTest.invoke(message)).isNull()
    }
}