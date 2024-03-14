package mega.privacy.android.domain.usecase.chat.message.paging

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.ChatMessageInfo
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
import org.junit.jupiter.params.provider.EnumSource
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
    fun `test that show avatar is false for my messages`() = runTest {
        val myMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(myMessage),
            currentUserHandle = myHandle,
            nextMessage = null,
        )

        assertThat(actual.all { !it.shouldShowAvatar }).isTrue()
    }

    @Test
    fun `test that showAvatar is true for a single message with no next message`() = runTest {
        val notMyMessage = mock<ChatMessage> {
            on { userHandle }.thenReturn(myHandle + 1)
            on { type }.thenReturn(ChatMessageType.NORMAL)
        }

        val actual = underTest(
            chatId = chatId,
            chatMessages = listOf(notMyMessage),
            currentUserHandle = myHandle,
            nextMessage = null,
        )

        assertThat(actual.all { it.shouldShowAvatar }).isTrue()
    }

    @Test
    fun `test that avatar is true only for second message of two with the same user handle and no next message`() =
        runTest {
            val firstMessage = mock<ChatMessage> {
                on { messageId }.thenReturn(1L)
                on { userHandle }.thenReturn(myHandle + 1)
                on { type }.thenReturn(ChatMessageType.NORMAL)
                on { timestamp }.thenReturn(1L)
            }

            val secondMessage = mock<ChatMessage> {
                on { messageId }.thenReturn(2L)
                on { userHandle }.thenReturn(myHandle + 1)
                on { type }.thenReturn(ChatMessageType.NORMAL)
                on { timestamp }.thenReturn(2L)
            }

            val actual = underTest(
                chatId = chatId,
                chatMessages = listOf(firstMessage, secondMessage),
                currentUserHandle = myHandle,
                nextMessage = null,
            ).associateBy(
                keySelector = { it.messageId },
                valueTransform = { it.shouldShowAvatar }
            )

            assertThat(actual[1L]).isFalse()
            assertThat(actual[2L]).isTrue()
        }


    @Test
    fun `test that show avatar is false for a single message with a next message with the same user handle`() =
        runTest {
            val notMyHandle = myHandle + 1L
            val notMyMessage = mock<ChatMessage> {
                on { messageId }.thenReturn(1L)
                on { userHandle }.thenReturn(notMyHandle)
                on { type }.thenReturn(ChatMessageType.NORMAL)
                on { timestamp }.thenReturn(1L)
            }

            val actual = underTest(
                chatId = chatId,
                chatMessages = listOf(notMyMessage),
                currentUserHandle = myHandle,
                nextMessage = createNextMessage(notMyHandle),
            ).first()

            assertThat(actual.shouldShowAvatar).isFalse()
        }

    private fun createNextMessage(
        notMyHandle: Long,
        chatMessageType: ChatMessageType = ChatMessageType.NORMAL,
    ): ChatMessageInfo =
        mock {
            on { userHandle }.thenReturn(notMyHandle)
            on { type }.thenReturn(chatMessageType)
        }

    @Test
    internal fun `test that show avatar is true for multiple single messages interspersed`() =
        runTest {
            val notMyHandle = myHandle + 1L
            val input = (1L..20L).map { value ->
                if (value % 2 == 0L) {
                    mock<ChatMessage> {
                        on { messageId }.thenReturn(value)
                        on { userHandle }.thenReturn(notMyHandle)
                        on { type }.thenReturn(ChatMessageType.NORMAL)
                        on { timestamp }.thenReturn(value)
                    }
                } else {
                    mock<ChatMessage> {
                        on { messageId }.thenReturn(value)
                        on { userHandle }.thenReturn(myHandle)
                        on { type }.thenReturn(ChatMessageType.NORMAL)
                        on { timestamp }.thenReturn(value)
                    }
                }
            }

            val actual = underTest(
                chatId = chatId,
                chatMessages = input,
                currentUserHandle = myHandle,
                nextMessage = createNextMessage(myHandle),
            )

            assertWithMessage("Show avatar should be true for not my messages")
                .that(actual.filter { it.userHandle == notMyHandle }
                    .all { it.shouldShowAvatar }).isTrue()

            assertWithMessage("Show avatar should be false for my messages")
                .that(actual.filter { it.userHandle == myHandle }
                    .none { it.shouldShowAvatar }).isTrue()

        }

    @ParameterizedTest(name = "Show avatar if next message is {0}")
    @EnumSource(
        value = ChatMessageType::class, names = [
            "NORMAL",
            "NODE_ATTACHMENT",
            "CONTACT_ATTACHMENT",
            "CONTAINS_META",
            "VOICE_CLIP",
        ], mode = EnumSource.Mode.EXCLUDE
    )
    internal fun `test that show avatar is true if next message is a management message`(ignoredType: ChatMessageType) =
        runTest {
            val notMyHandle = myHandle + 1L

            val firstMessage = mock<ChatMessage> {
                on { messageId }.thenReturn(1L)
                on { userHandle }.thenReturn(notMyHandle)
                on { type }.thenReturn(ChatMessageType.NORMAL)
                on { timestamp }.thenReturn(1L)
            }

            val secondMessage = mock<ChatMessage> {
                on { messageId }.thenReturn(2L)
                on { userHandle }.thenReturn(notMyHandle)
                on { type }.thenReturn(ignoredType)
                on { timestamp }.thenReturn(2L)
            }

            val actual = underTest(
                chatId = chatId,
                chatMessages = listOf(firstMessage, secondMessage),
                currentUserHandle = myHandle,
                nextMessage = createNextMessage(myHandle),
            ).associateBy(
                keySelector = { it.messageId },
                valueTransform = { it.shouldShowAvatar }
            )

            assertThat(actual[1L]).isTrue()
            assertThat(actual[2L]).isFalse()
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
            nextMessage = null,
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
            nextMessage = null,
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
            nextMessage = null,
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
            nextMessage = null,
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
            nextMessage = null,
        )
        assertThat(actual.map { it.exists }).containsExactly(exists)
    }

    @Test
    internal fun `test that an empty list completes successfully`() = runTest {
        val actual = underTest(
            chatId = chatId,
            chatMessages = emptyList(),
            currentUserHandle = myHandle,
            nextMessage = null,
        )

        assertThat(actual).isEmpty()
    }
}