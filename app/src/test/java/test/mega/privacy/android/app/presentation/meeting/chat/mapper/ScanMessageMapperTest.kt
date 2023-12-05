package test.mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.meeting.chat.mapper.ScanMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.UiChatMessageMapper
import mega.privacy.android.app.presentation.meeting.chat.model.ui.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanMessageMapperTest {
    private val uiChatMessageMapper: UiChatMessageMapper = mock()
    private lateinit var underTest: ScanMessageMapper

    @BeforeAll
    fun setUp() {
        underTest = ScanMessageMapper(uiChatMessageMapper)
    }

    @Test
    fun `test that the new message is added to the list when there is no latest message`() {
        val newMessage = mock<TypedMessage> {
            on { isMine } doReturn false
            on { userHandle } doReturn 1234567890L
        }
        val uiMessage = mock<UiChatMessage>()
        whenever(
            uiChatMessageMapper(
                newMessage,
                isOneToOne = true,
                showAvatar = true,
                showTime = true,
                showDate = true
            )
        ).thenReturn(
            uiMessage
        )
        val result = underTest(
            isOneToOne = true,
            currentItems = emptyList(),
            newMessage = newMessage
        )
        Truth.assertThat(result).hasSize(1)
        Truth.assertThat(result[0]).isEqualTo(uiMessage)
    }

    @Test
    fun `test that the new message is added to the list`() {
        val oldTypedMessage = mock<TypedMessage> {
            on { isMine } doReturn false
            on { userHandle } doReturn 9876543210L
        }
        val oldUiMessage = mock<UiChatMessage> {
            on { showAvatar } doReturn true
            on { message } doReturn oldTypedMessage
        }
        val newMessage = mock<TypedMessage> {
            on { isMine } doReturn false
            on { userHandle } doReturn 1234567890L
        }
        val newUiMessage = mock<UiChatMessage> {
            on { showAvatar } doReturn false
            on { message } doReturn newMessage
        }
        val currentItems = listOf(oldUiMessage)
        whenever(
            uiChatMessageMapper(
                message = eq(value = newMessage),
                isOneToOne = any(),
                showAvatar = any(),
                showTime = any(),
                showDate = any()
            )
        ).thenReturn(newUiMessage)
        whenever(
            uiChatMessageMapper(
                message = eq(value = oldTypedMessage),
                isOneToOne = any(),
                showAvatar = any(),
                showTime = any(),
                showDate = any()
            )
        ).thenReturn(oldUiMessage)
        val result = underTest(
            isOneToOne = true,
            currentItems = currentItems,
            newMessage = newMessage
        )
        Truth.assertThat(result).hasSize(2)
        Truth.assertThat(result).isEqualTo(listOf(newUiMessage) + currentItems)
    }
}