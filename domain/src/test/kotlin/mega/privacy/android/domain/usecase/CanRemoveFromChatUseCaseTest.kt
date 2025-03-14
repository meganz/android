package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.usecase.chat.GetChatMessageUseCase
import mega.privacy.android.domain.usecase.mediaplayer.videoplayer.CanRemoveFromChatUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CanRemoveFromChatUseCaseTest {
    private lateinit var underTest: CanRemoveFromChatUseCase
    private val getChatMessageUseCase = mock<GetChatMessageUseCase>()
    private val chatRepository = mock<ChatRepository>()
    private val testChatId = 1L
    private val testMessageId = 2L
    private val testUserHandle = 3L

    @BeforeAll
    fun setUp() {
        underTest = CanRemoveFromChatUseCase(
            getChatMessageUseCase = getChatMessageUseCase,
            chatRepository = chatRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(getChatMessageUseCase, chatRepository)
    }

    @Test
    fun `test that returns false when message is null`() = runTest {
        whenever(getChatMessageUseCase(testChatId, testMessageId)).thenReturn(null)
        whenever(chatRepository.getMyUserHandle()).thenReturn(testUserHandle)
        val actual = underTest(testChatId, testMessageId)
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that returns false when userHandle is not myUserHandle`() = runTest {
        val mockMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(testUserHandle)
        }
        whenever(getChatMessageUseCase(testChatId, testMessageId)).thenReturn(mockMessage)
        whenever(chatRepository.getMyUserHandle()).thenReturn(4L)
        val actual = underTest(testChatId, testMessageId)
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that returns false when message is deletable`() = runTest {
        val mockMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(testUserHandle)
            on { isDeletable }.thenReturn(false)
        }
        whenever(getChatMessageUseCase(testChatId, testMessageId)).thenReturn(mockMessage)
        whenever(chatRepository.getMyUserHandle()).thenReturn(testUserHandle)
        val actual = underTest(testChatId, testMessageId)
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that returns true`() = runTest {
        val mockMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(testUserHandle)
            on { isDeletable }.thenReturn(true)
        }
        whenever(getChatMessageUseCase(testChatId, testMessageId)).thenReturn(mockMessage)
        whenever(chatRepository.getMyUserHandle()).thenReturn(testUserHandle)
        val actual = underTest(testChatId, testMessageId)
        assertThat(actual).isTrue()
    }
}