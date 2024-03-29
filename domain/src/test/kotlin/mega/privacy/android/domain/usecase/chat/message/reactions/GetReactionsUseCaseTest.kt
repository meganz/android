package mega.privacy.android.domain.usecase.chat.message.reactions

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
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
class GetReactionsUseCaseTest {

    private lateinit var underTest: GetReactionsUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    fun setup() {
        underTest = GetReactionsUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
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
        val messageReaction1 = Reaction(reaction1, count1, users1, true)
        val messageReaction2 = Reaction(reaction2, count2, users2, true)
        val messageReaction3 = Reaction(reaction3, count3, users3, false)
        val expectedList = listOf(messageReaction1, messageReaction2, messageReaction3)
        whenever(chatMessageRepository.getMessageReactions(chatId, msgId)).thenReturn(reactionsList)
        whenever(chatMessageRepository.getMessageReactionCount(chatId, msgId, reaction1))
            .thenReturn(count1)
        whenever(chatMessageRepository.getReactionUsers(chatId, msgId, reaction1)).thenReturn(users1)
        whenever(chatMessageRepository.getMessageReactionCount(chatId, msgId, reaction2))
            .thenReturn(count2)
        whenever(chatMessageRepository.getReactionUsers(chatId, msgId, reaction2)).thenReturn(users2)
        whenever(chatMessageRepository.getMessageReactionCount(chatId, msgId, reaction3))
            .thenReturn(count3)
        whenever(chatMessageRepository.getReactionUsers(chatId, msgId, reaction3)).thenReturn(users3)
        Truth.assertThat(underTest.invoke(chatId, msgId, myUserHandle)).isEqualTo(expectedList)
        verify(chatMessageRepository).getMessageReactions(chatId, msgId)
        verify(chatMessageRepository).getMessageReactionCount(chatId, msgId, reaction1)
        verify(chatMessageRepository).getReactionUsers(chatId, msgId, reaction1)
        verify(chatMessageRepository).getMessageReactionCount(chatId, msgId, reaction2)
        verify(chatMessageRepository).getReactionUsers(chatId, msgId, reaction2)
        verify(chatMessageRepository).getMessageReactionCount(chatId, msgId, reaction3)
        verify(chatMessageRepository).getReactionUsers(chatId, msgId, reaction3)
        verifyNoMoreInteractions(chatMessageRepository)
    }
}