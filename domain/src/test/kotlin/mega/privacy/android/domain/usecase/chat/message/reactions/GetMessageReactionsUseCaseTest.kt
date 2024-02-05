package mega.privacy.android.domain.usecase.chat.message.reactions

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.reactions.MessageReaction
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetMessageReactionsUseCaseTest {

    private lateinit var underTest: GetMessageReactionsUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeEach
    fun setup() {
        underTest = GetMessageReactionsUseCase(chatRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that get message reactions use case invokes and returns correctly`() = runTest {
        val chatId = 1L
        val msgId = 2L
        val myUserHandle = 3L
        val user2 = 4L
        val reaction1 = "reaction1"
        val count1 = 2
        val users1 = listOf(myUserHandle, user2)
        val reaction2 = "reaction2"
        val count2 = 1
        val users2 = listOf(myUserHandle)
        val reaction3 = "reaction3"
        val count3 = 1
        val users3 = listOf(user2)
        val reactionsList = listOf(reaction1, reaction2, reaction3)
        val messageReaction1 = MessageReaction(reaction1, count1, users1, true)
        val messageReaction2 = MessageReaction(reaction2, count2, users2, true)
        val messageReaction3 = MessageReaction(reaction3, count3, users3, false)
        val expectedList = listOf(messageReaction1, messageReaction2, messageReaction3)
        whenever(chatRepository.getMessageReactions(chatId, msgId)).thenReturn(reactionsList)
        whenever(chatRepository.getMessageReactionCount(chatId, msgId, reaction1))
            .thenReturn(count1)
        whenever(chatRepository.getReactionUsers(chatId, msgId, reaction1)).thenReturn(users1)
        whenever(chatRepository.getMessageReactionCount(chatId, msgId, reaction2))
            .thenReturn(count2)
        whenever(chatRepository.getReactionUsers(chatId, msgId, reaction2)).thenReturn(users2)
        whenever(chatRepository.getMessageReactionCount(chatId, msgId, reaction3))
            .thenReturn(count3)
        whenever(chatRepository.getReactionUsers(chatId, msgId, reaction3)).thenReturn(users3)
        Truth.assertThat(underTest.invoke(chatId, msgId, myUserHandle)).isEqualTo(expectedList)
        verify(chatRepository).getMessageReactions(chatId, msgId)
        verify(chatRepository).getMessageReactionCount(chatId, msgId, reaction1)
        verify(chatRepository).getReactionUsers(chatId, msgId, reaction1)
        verify(chatRepository).getMessageReactionCount(chatId, msgId, reaction2)
        verify(chatRepository).getReactionUsers(chatId, msgId, reaction2)
        verify(chatRepository).getMessageReactionCount(chatId, msgId, reaction3)
        verify(chatRepository).getReactionUsers(chatId, msgId, reaction3)
        verifyNoMoreInteractions(chatRepository)
    }
}