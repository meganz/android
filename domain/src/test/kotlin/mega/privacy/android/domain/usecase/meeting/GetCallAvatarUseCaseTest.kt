package mega.privacy.android.domain.usecase.meeting

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.repository.AvatarRepository
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.contact.GetUserFirstName
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCallAvatarUseCaseTest {
    private val avatarRepository = mock<AvatarRepository>()
    private val getUserFirstName = mock<GetUserFirstName>()
    private val getChatRoomUseCase = mock<GetChatRoomUseCase>()

    private lateinit var underTest: GetCallAvatarUseCase

    val chatId = 123L
    private val callerHandle = 456L
    private val chatTitle = "Chat title"

    private val group = mock<ChatRoom> {
        on { chatId }.thenReturn(chatId)
        on { changes }.thenReturn(null)
        on { title }.thenReturn(chatTitle)
        on { isGroup }.thenReturn(true)
        on { isMeeting }.thenReturn(false)
        on { isArchived }.thenReturn(false)
        on { isActive }.thenReturn(true)
    }
    private val meeting = mock<ChatRoom> {
        on { chatId }.thenReturn(chatId)
        on { changes }.thenReturn(null)
        on { title }.thenReturn(chatTitle)
        on { isGroup }.thenReturn(false)
        on { isMeeting }.thenReturn(true)
        on { isArchived }.thenReturn(false)
        on { isActive }.thenReturn(true)
    }

    @BeforeEach
    fun setup() {
        underTest = GetCallAvatarUseCase(getChatRoomUseCase, avatarRepository, getUserFirstName)
    }

    @Test
    fun `test that a ChatAvatarItem for group is returned`() =
        runTest {
            whenever(getChatRoomUseCase(chatId)).thenReturn(group)

            val actual = underTest.invoke(chatId = chatId, callerHandle = callerHandle)
            val expected =
                ChatAvatarItem(placeholderText = chatTitle, uri = null, color = null)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that a ChatAvatarItem for meeting is returned`() =
        runTest {
            whenever(getChatRoomUseCase(chatId)).thenReturn(meeting)

            val actual = underTest.invoke(chatId = chatId, callerHandle = callerHandle)
            val expected =
                ChatAvatarItem(placeholderText = chatTitle, uri = null, color = null)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that a ChatAvatarItem for one to one is returned`() =
        runTest {

            val callerName = "Name"
            val uri = "avatar_file"
            val file = File(uri)
            val absolutePath = file.absolutePath
            val oneToOne = mock<ChatRoom> {
                on { chatId }.thenReturn(chatId)
                on { changes }.thenReturn(null)
                on { title }.thenReturn(callerName)
                on { isGroup }.thenReturn(false)
                on { isMeeting }.thenReturn(false)
                on { isArchived }.thenReturn(false)
                on { isActive }.thenReturn(true)
            }

            val color = 0xFFFFFFFF.toInt()

            whenever(getChatRoomUseCase(chatId)).thenReturn(oneToOne)
            whenever(
                getUserFirstName(
                    callerHandle,
                    skipCache = false,
                    shouldNotify = false
                )
            ).thenReturn(callerName)

            whenever(avatarRepository.getAvatarColor(callerHandle)).thenReturn(color)
            whenever(avatarRepository.getAvatarFile(callerHandle)).thenReturn(file)

            val actual = underTest.invoke(chatId = chatId, callerHandle = callerHandle)
            val expected =
                ChatAvatarItem(
                    placeholderText = callerName,
                    uri = absolutePath,
                    color = color
                )

            assertThat(actual).isEqualTo(expected)
        }
}