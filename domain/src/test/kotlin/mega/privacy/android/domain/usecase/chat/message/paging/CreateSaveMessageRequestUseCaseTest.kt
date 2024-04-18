package mega.privacy.android.domain.usecase.chat.message.paging

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.chat.message.GetExistsInMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.reactions.GetReactionsUseCase
import mega.privacy.android.domain.usecase.node.DoesNodeExistUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateSaveMessageRequestUseCaseTest {

    private lateinit var underTest: CreateSaveMessageRequestUseCase

    private val getReactionsUseCase = mock<GetReactionsUseCase>()
    private val doesNodeExistUseCase = mock<DoesNodeExistUseCase>()
    private val getExistsInMessageUseCase = mock<GetExistsInMessageUseCase>()

    private val myHandle = 123L
    private val chatId = 456L

    @BeforeAll
    internal fun setUp() {
        underTest = CreateSaveMessageRequestUseCase(
            getReactionsUseCase,
            doesNodeExistUseCase,
            getExistsInMessageUseCase
        )
    }

    @BeforeEach
    internal fun resetMocks() {
        reset(
            getReactionsUseCase,
            doesNodeExistUseCase,
            getExistsInMessageUseCase,
        )
    }

    @Test
    fun `test that reactions are retrieved for messages with confirmed reactions`() = runTest {
        val message = mock<ChatMessage> {
            on { hasConfirmedReactions }.thenReturn(true)
        }
        val reaction1 = mock<Reaction>()
        val reaction2 = mock<Reaction>()
        val reactions = listOf(reaction1, reaction2)
        whenever(getReactionsUseCase(chatId, message.messageId, myHandle)).thenReturn(reactions)

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(message),
            currentUserHandle = myHandle,
        )
        assertThat(actual.map { it.reactions }).containsExactly(reactions)
    }

    @Test
    fun `test that reactions are not for messages without confirmed reactions`() = runTest {
        val message = mock<ChatMessage> {
            on { hasConfirmedReactions }.thenReturn(false)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(message),
            currentUserHandle = myHandle,
        )
        assertThat(actual.map { it.reactions }).containsExactly(emptyList<Reaction>())
        verifyNoInteractions(getReactionsUseCase)
    }


    @Test
    fun `test that exists is true if node list is null`() = runTest {
        val message = mock<ChatMessage> {
            on { nodeList }.thenReturn(emptyList())
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(message),
            currentUserHandle = myHandle,
        )

        assertThat(actual.map { it.exists }).containsExactly(true)
        verifyNoInteractions(doesNodeExistUseCase)
    }

    @ParameterizedTest(name = " when get exists in message returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that exists has correct value if node list is not null and message is not mine`(
        exists: Boolean,
    ) = runTest {
        val node = mock<Node>()
        val msgId = 456L
        val message = mock<ChatMessage> {
            on { messageId } doReturn msgId
            on { nodeList } doReturn listOf(node)
            on { userHandle } doReturn 789L
        }
        whenever(getExistsInMessageUseCase(chatId, msgId)).thenReturn(exists)
        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(message),
            currentUserHandle = myHandle,
        )
        assertThat(actual.map { it.exists }).containsExactly(exists)
    }

    @ParameterizedTest(name = " when does not exists use case returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that exists has correct value if node list is not null and message is mine`(
        exists: Boolean,
    ) = runTest {
        val nodeId = NodeId(123L)
        val node = mock<Node> {
            on { id }.thenReturn(nodeId)
        }
        val message = mock<ChatMessage> {
            on { nodeList } doReturn listOf(node)
            on { userHandle } doReturn myHandle
        }
        whenever(doesNodeExistUseCase(nodeId)).thenReturn(exists)
        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(message),
            currentUserHandle = myHandle,
        )
        assertThat(actual.map { it.exists }).containsExactly(exists)
    }

    @Test
    internal fun `test that an empty list completes successfully`() = runTest {
        val actual = underTest(
            chatId = chatId,
            chatMessages = emptyList(),
            currentUserHandle = myHandle,
        )

        assertThat(actual).isEmpty()
    }
}