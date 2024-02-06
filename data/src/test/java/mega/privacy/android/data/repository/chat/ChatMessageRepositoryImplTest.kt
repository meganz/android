package mega.privacy.android.data.repository.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.handles.HandleListMapper
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaStringList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatMessageRepositoryImplTest {

    private lateinit var underTest: ChatMessageRepositoryImpl

    private val megaChatApiGateway: MegaChatApiGateway = mock()
    private val stringListMapper = mock<StringListMapper>()
    private val handleListMapper = mock<HandleListMapper>()
    private val chatMessageMapper = mock<ChatMessageMapper>()

    private val megaChatErrorSuccess = mock<MegaChatError> {
        on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
    }
    private val chatId = 123L
    private val msgId = 456L

    @BeforeAll
    fun setUp() {
        underTest = ChatMessageRepositoryImpl(
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            stringListMapper = stringListMapper,
            handleListMapper = handleListMapper,
            chatMessageMapper = chatMessageMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaChatApiGateway
        )
    }

    @Test
    fun `test that setMessageSeen invoke correctly`() = runTest {
        val messageId = 2L
        underTest.setMessageSeen(chatId, messageId)
        verify(megaChatApiGateway).setMessageSeen(chatId, messageId)
    }

    @Test
    fun `test that getLastMessageSeenId invoke correctly`() = runTest {
        underTest.getLastMessageSeenId(chatId)
        verify(megaChatApiGateway).getLastMessageSeenId(chatId)
    }

    @Test
    fun `test that add reaction invokes correctly`() = runTest {
        val reaction = "reaction"
        whenever(megaChatApiGateway.addReaction(any(), any(), any(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaChatErrorSuccess,
            )
        }
        underTest.addReaction(chatId, msgId, reaction)
        verify(megaChatApiGateway).addReaction(eq(chatId), eq(msgId), eq(reaction), any())
        verifyNoMoreInteractions(megaChatApiGateway)
    }

    @Test
    fun `test that delete reaction invokes correctly`() = runTest {
        val reaction = "reaction"
        whenever(megaChatApiGateway.delReaction(any(), any(), any(), any())).thenAnswer {
            ((it.arguments[3]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaChatErrorSuccess,
            )
        }
        underTest.deleteReaction(chatId, msgId, reaction)
        verify(megaChatApiGateway).delReaction(eq(chatId), eq(msgId), eq(reaction), any())
        verifyNoMoreInteractions(megaChatApiGateway)
    }

    @Test
    fun `test that get message reactions invokes and returns correctly`() = runTest {
        val reaction1 = "reaction1"
        val reaction2 = "reaction2"
        val reaction3 = "reaction3"
        val reactions = mock<MegaStringList> {
            on { size() } doReturn 3
            on { get(0) } doReturn reaction1
            on { get(1) } doReturn reaction2
            on { get(2) } doReturn reaction3
        }
        val reactionsList = listOf(reaction1, reaction2, reaction3)
        whenever(megaChatApiGateway.getMessageReactions(chatId, msgId)).thenReturn(reactions)
        whenever(stringListMapper(reactions)).thenReturn(reactionsList)
        assertThat(underTest.getMessageReactions(chatId, msgId)).isEqualTo(reactionsList)
        verify(megaChatApiGateway).getMessageReactions(chatId, msgId)
        verifyNoMoreInteractions(megaChatApiGateway)
    }

    @Test
    fun `test that get message reaction count invokes and returns correctly`() = runTest {
        val reaction = "reaction"
        val count = 3
        whenever(megaChatApiGateway.getMessageReactionCount(any(), any(), any())).thenReturn(count)
        assertThat(underTest.getMessageReactionCount(chatId, msgId, reaction)).isEqualTo(count)
        verify(megaChatApiGateway).getMessageReactionCount(chatId, msgId, reaction)
        verifyNoMoreInteractions(megaChatApiGateway)
    }

    @Test
    fun `test that get reaction users invokes and returns correctly`() = runTest {
        val reaction = "reaction"
        val user1 = 5L
        val user2 = 6L
        val user3 = 7L
        val users = mock<MegaHandleList> {
            on { size() } doReturn 3
            on { get(0) } doReturn user1
            on { get(1) } doReturn user2
            on { get(2) } doReturn user3
        }
        val usersList = listOf(user1, user2, user3)
        whenever(megaChatApiGateway.getReactionUsers(any(), any(), any())).thenReturn(users)
        whenever(handleListMapper(users)).thenReturn(usersList)
        assertThat(underTest.getReactionUsers(chatId, msgId, reaction)).isEqualTo(usersList)
        verify(megaChatApiGateway).getReactionUsers(chatId, msgId, reaction)
        verifyNoMoreInteractions(megaChatApiGateway)
    }

    @Test
    fun `test that send giphy invokes chat api`() = runTest {
        val srcMp4 = "srcMp4"
        val srcWebp = "srcWebp"
        val sizeMp4 = 350L
        val sizeWebp = 250L
        val width = 250
        val height = 500
        val title = "title"
        whenever(
            megaChatApiGateway.sendGiphy(
                chatId = chatId,
                srcMp4 = srcMp4,
                srcWebp = srcWebp,
                sizeMp4 = sizeMp4,
                sizeWebp = sizeWebp,
                width = width,
                height = height,
                title = title
            )
        ).thenReturn(mock())
        underTest.sendGiphy(
            chatId = chatId,
            srcMp4 = srcMp4,
            srcWebp = srcWebp,
            sizeMp4 = sizeMp4,
            sizeWebp = sizeWebp,
            width = width,
            height = height,
            title = title
        )
        verify(megaChatApiGateway).sendGiphy(
            chatId = chatId,
            srcMp4 = srcMp4,
            srcWebp = srcWebp,
            sizeMp4 = sizeMp4,
            sizeWebp = sizeWebp,
            width = width,
            height = height,
            title = title
        )
    }
}