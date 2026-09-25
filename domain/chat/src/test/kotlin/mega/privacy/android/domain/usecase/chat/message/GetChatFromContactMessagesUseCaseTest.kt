package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatFromContactMessagesUseCaseTest {

    private lateinit var underTest: GetChatFromContactMessagesUseCase

    private val get1On1ChatIdUseCase = mock<Get1On1ChatIdUseCase>()
    private val createGroupChatRoomUseCase = mock<CreateGroupChatRoomUseCase>()

    private val chatId = 456L
    private val contactHandle = 123L
    private val email1 = "email1"
    private val message = mock<ContactAttachmentMessage> {
        on { this.contactHandle } doReturn contactHandle
        on { this.contactEmail } doReturn email1
    }

    @BeforeEach
    fun setup() {
        underTest = GetChatFromContactMessagesUseCase(
            get1On1ChatIdUseCase,
            createGroupChatRoomUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            get1On1ChatIdUseCase,
            createGroupChatRoomUseCase
        )
    }

    @Test
    fun `test that get chat from contact messages invokes and returns correctly if messages size is one`() =
        runTest {
            whenever(get1On1ChatIdUseCase(contactHandle)).thenReturn(chatId)
            underTest.invoke(listOf(message)).let {
                Truth.assertThat(it).isEqualTo(chatId)
            }
        }

    @Test
    fun `test that get chat from contact messages invokes and returns correctly if messages size more than one`() =
        runTest {
            val email2 = "email2"
            val message2 = mock<ContactAttachmentMessage> {
                on { this.contactEmail } doReturn email2
            }
            whenever(
                createGroupChatRoomUseCase(
                    emails = listOf(email1, email2),
                    title = null,
                    isEkr = false,
                    addParticipants = true,
                    chatLink = false
                )
            ).thenReturn(chatId)
            underTest.invoke(listOf(message, message2)).let {
                Truth.assertThat(it).isEqualTo(chatId)
            }
        }
}