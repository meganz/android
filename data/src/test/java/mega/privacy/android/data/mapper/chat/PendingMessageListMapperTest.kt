package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage
import mega.privacy.android.domain.entity.chat.PendingMessage
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PendingMessageListMapperTest {

    private lateinit var underTest: PendingMessageListMapper

    @BeforeAll
    fun setup() {
        underTest = PendingMessageListMapper()
    }

    @Test
    fun `test that pending messag elist mapper returns correctly`() {
        val pendingMessage1 = mock<PendingMessage>()
        val pendingMessage2 = mock<PendingMessage>()
        val pendingMessage3 = mock<PendingMessage>()
        val message1 = mock<AndroidMegaChatMessage> {
            on { pendingMessage }.thenReturn(null)
        }
        val message2 = mock<AndroidMegaChatMessage> {
            on { pendingMessage }.thenReturn(pendingMessage1)
        }
        val messages3 = mock<AndroidMegaChatMessage> {
            on { pendingMessage }.thenReturn(pendingMessage2)
        }
        val message4 = mock<AndroidMegaChatMessage> {
            on { pendingMessage }.thenReturn(null)
        }
        val messages5 = mock<AndroidMegaChatMessage> {
            on { pendingMessage }.thenReturn(pendingMessage3)
        }
        val messagesList = listOf(message1, message2, messages3, message4, messages5)
        val pendingMessageList = listOf(pendingMessage1, pendingMessage2, pendingMessage3)
        Truth.assertThat(underTest.invoke(messagesList)).isEqualTo(pendingMessageList)
    }
}