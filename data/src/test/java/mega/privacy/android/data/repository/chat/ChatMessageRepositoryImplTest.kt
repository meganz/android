package mega.privacy.android.data.repository.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.cache.Cache
import mega.privacy.android.data.database.converter.TypedMessageEntityConverters
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.chat.ChatStorageGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.mapper.StringListMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.chat.messages.PendingMessageEntityMapper
import mega.privacy.android.data.mapper.chat.messages.PendingMessageMapper
import mega.privacy.android.data.mapper.chat.paging.TypedMessagePagingSourceMapper
import mega.privacy.android.data.mapper.handles.HandleListMapper
import mega.privacy.android.data.mapper.handles.MegaHandleListMapper
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.messages.pending.SavePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import mega.privacy.android.domain.entity.node.NodeId
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaUser
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
    private val megaApiGateway = mock<MegaApiGateway>()
    private val stringListMapper = mock<StringListMapper>()
    private val handleListMapper = mock<HandleListMapper>()
    private val chatMessageMapper = mock<ChatMessageMapper>()
    private val megaHandleListMapper = mock<MegaHandleListMapper>()
    private val chatStorageGateway = mock<ChatStorageGateway>()
    private val pendingMessageEntityMapper = mock<PendingMessageEntityMapper>()
    private val pendingMessageMapper = mock<PendingMessageMapper>()
    private val typedMessageEntityConverters = mock<TypedMessageEntityConverters>()
    private val originalPathCache = mock<Cache<Map<NodeId, String>>>()
    private val typedMessagePagingSourceMapper = mock<TypedMessagePagingSourceMapper>()
    private val megaChatErrorSuccess = mock<MegaChatError> {
        on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
    }
    private val chatId = 123L
    private val msgId = 456L


    @BeforeAll
    fun setUp() {
        underTest = ChatMessageRepositoryImpl(
            megaChatApiGateway = megaChatApiGateway,
            megaApiGateway = megaApiGateway,
            chatStorageGateway = chatStorageGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            stringListMapper = stringListMapper,
            handleListMapper = handleListMapper,
            chatMessageMapper = chatMessageMapper,
            megaHandleListMapper = megaHandleListMapper,
            pendingMessageEntityMapper = pendingMessageEntityMapper,
            pendingMessageMapper = pendingMessageMapper,
            typedMessageEntityConverters = typedMessageEntityConverters,
            originalPathCache = originalPathCache,
            typedMessagePagingSourceMapper = typedMessagePagingSourceMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaChatApiGateway,
            chatStorageGateway,
            stringListMapper,
            handleListMapper,
            chatMessageMapper,
            megaHandleListMapper,
            pendingMessageEntityMapper,
            pendingMessageMapper,
            typedMessageEntityConverters,
            originalPathCache,
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

    @Test
    fun `test that attach contact invokes apis and mappers correctly`() = runTest {
        val contactEmail = "contactEmail"
        val contactHandle = 123L
        val user = mock<MegaUser> {
            on { handle } doReturn contactHandle
        }
        val userList = listOf(contactHandle)
        val handleList = mock<MegaHandleList>()
        val message = mock<MegaChatMessage>()
        val chatMessage = mock<ChatMessage>()
        whenever(megaApiGateway.getContact(contactEmail)).thenReturn(user)
        whenever(megaHandleListMapper(userList)).thenReturn(handleList)
        whenever(megaChatApiGateway.attachContacts(chatId, handleList)).thenReturn(message)
        whenever(chatMessageMapper(message)).thenReturn(chatMessage)
        assertThat(underTest.attachContact(chatId, contactEmail)).isEqualTo(chatMessage)
        verify(megaApiGateway).getContact(contactEmail)
        verify(megaHandleListMapper).invoke(userList)
        verify(megaChatApiGateway).attachContacts(chatId, handleList)
        verify(chatMessageMapper).invoke(message)
    }

    @Test
    fun `test that savePendingMessage saves the mapped entities`() = runTest {
        val id = 19L
        val savePendingMessageRequest = mock<SavePendingMessageRequest> {
            on { state } doReturn mock()
            on { filePath } doReturn "file"
        }
        val pendingMessageEntity = mock<PendingMessageEntity>()
        whenever(pendingMessageEntityMapper(savePendingMessageRequest))
            .thenReturn(pendingMessageEntity)
        whenever(chatStorageGateway.storePendingMessage(pendingMessageEntity))
            .thenReturn(id)
        val actual = underTest.savePendingMessage(savePendingMessageRequest)
        assertThat(actual.id).isEqualTo(id)
    }

    @Test
    fun `test that updatePendingMessage saves the mapped entities`() = runTest {
        val savePendingMessageRequest = mock<SavePendingMessageRequest> {
            on { state } doReturn mock()
            on { filePath } doReturn "file"
        }
        val pendingMessageEntity = mock<PendingMessageEntity>()
        whenever(pendingMessageEntityMapper(savePendingMessageRequest))
            .thenReturn(pendingMessageEntity)
        underTest.updatePendingMessage(savePendingMessageRequest)
        verify(chatStorageGateway).updatePendingMessage(pendingMessageEntity)
    }

    @Test
    fun `test that forward contact invokes api and returns the message`() = runTest {
        val chatMessage = mock<MegaChatMessage>()
        val message = mock<ChatMessage>()
        whenever(megaChatApiGateway.forwardContact(any(), any(), any())).thenReturn(chatMessage)
        whenever(chatMessageMapper(chatMessage)).thenReturn(message)
        assertThat(underTest.forwardContact(any(), any(), any())).isEqualTo(message)
        verify(megaChatApiGateway).forwardContact(any(), any(), any())
        verify(chatMessageMapper).invoke(chatMessage)
    }

    @Test
    fun `test that attachNode invokes megaApi and returns the temp id`() = runTest {
        val handle = 2L
        val expectedTempId = 3L
        val message = mock<MegaChatMessage> { on { tempId }.thenReturn(expectedTempId) }
        val megaChatRequest = mock<MegaChatRequest> { on { megaChatMessage }.thenReturn(message) }
        whenever(megaChatApiGateway.attachNode(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequest,
                megaChatErrorSuccess,
            )
        }
        val actual = underTest.attachNode(chatId, handle)
        assertThat(actual).isEqualTo(expectedTempId)
        verify(megaChatApiGateway).attachNode(any(), any(), any())
    }

    @Test
    fun `test that attachVoiceMessage invokes megaApi returns the temp id`() = runTest {

        val handle = 2L
        val expectedTempId = 3L
        val message = mock<MegaChatMessage> { on { tempId }.thenReturn(expectedTempId) }
        val megaChatRequest = mock<MegaChatRequest> { on { megaChatMessage }.thenReturn(message) }
        whenever(megaChatApiGateway.attachVoiceMessage(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequest,
                megaChatErrorSuccess,
            )
        }
        val actual = underTest.attachVoiceMessage(chatId, handle)
        assertThat(actual).isEqualTo(expectedTempId)
        verify(megaChatApiGateway).attachVoiceMessage(any(), any(), any())
    }

    @Test
    fun `test that getPendingMessage returns the mapped entity`() = runTest {
        val pendingMessageId = 2011L
        val entity = mock<PendingMessageEntity>()
        val expected = mock<PendingMessage>()
        whenever(chatStorageGateway.getPendingMessage(pendingMessageId)).thenReturn(entity)
        whenever(pendingMessageMapper(entity)).thenReturn(expected)
        val actual = underTest.getPendingMessage(pendingMessageId)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that get message ids by type invokes chat storage gateway correctly`() = runTest {
        val type = ChatMessageType.NODE_ATTACHMENT
        underTest.getMessageIdsByType(chatId, type)
        verify(chatStorageGateway).getMessageIdsByType(chatId, type)
    }

    @Test
    fun `test that get reactions invokes and returns correctly`() = runTest {
        val reactions = "reactions"
        val reactionsList = emptyList<Reaction>()
        whenever(chatStorageGateway.getMessageReactions(chatId, msgId)).thenReturn(reactions)
        whenever(typedMessageEntityConverters.convertToMessageReactionList(reactions))
            .thenReturn(reactionsList)
        assertThat(underTest.getReactionsFromMessage(chatId, msgId)).isEqualTo(reactionsList)
        verify(chatStorageGateway).getMessageReactions(chatId, msgId)
        verify(typedMessageEntityConverters).convertToMessageReactionList(reactions)
    }

    @Test
    fun `test that update reactions invokes correctly`() = runTest {
        val reactions = listOf<Reaction>()
        val reactionsString = "reactions"
        whenever(typedMessageEntityConverters.convertFromMessageReactionList(reactions))
            .thenReturn(reactionsString)
        whenever(chatStorageGateway.updateMessageReactions(chatId, msgId, reactionsString))
            .thenReturn(Unit)
        underTest.updateReactionsInMessage(chatId, msgId, reactions)
        verify(typedMessageEntityConverters).convertFromMessageReactionList(reactions)
        verify(chatStorageGateway).updateMessageReactions(chatId, msgId, reactionsString)
    }

    @Test
    fun `test that delete message invokes and returns correctly`() = runTest {
        val message = mock<MegaChatMessage>()
        val chatMessage = mock<ChatMessage>()
        whenever(megaChatApiGateway.deleteMessage(chatId, msgId)).thenReturn(message)
        whenever(chatMessageMapper(message)).thenReturn(chatMessage)
        assertThat(underTest.deleteMessage(chatId, msgId)).isEqualTo(chatMessage)
    }

    @Test
    fun `test that revoke attachment message invokes and returns correctly`() = runTest {
        val message = mock<MegaChatMessage>()
        val chatMessage = mock<ChatMessage>()
        whenever(megaChatApiGateway.revokeAttachmentMessage(chatId, msgId)).thenReturn(message)
        whenever(chatMessageMapper(message)).thenReturn(chatMessage)
        assertThat(underTest.revokeAttachmentMessage(chatId, msgId)).isEqualTo(chatMessage)
    }

    @Test
    fun `test that edit message invokes and returns correctly`() = runTest {
        val message = mock<MegaChatMessage>()
        val chatMessage = mock<ChatMessage>()
        val content = "content"
        whenever(megaChatApiGateway.editMessage(chatId, msgId, content)).thenReturn(message)
        whenever(chatMessageMapper(message)).thenReturn(chatMessage)
        assertThat(underTest.editMessage(chatId, msgId, content)).isEqualTo(chatMessage)
    }

    @Test
    fun `test that edit geolocation invokes and returns correctly`() = runTest {
        val message = mock<MegaChatMessage>()
        val chatMessage = mock<ChatMessage>()
        val latitude = 1F
        val longitude = 2F
        val img = "img"
        whenever(megaChatApiGateway.editGeolocation(chatId, msgId, longitude, latitude, img))
            .thenReturn(message)
        whenever(chatMessageMapper(message)).thenReturn(chatMessage)
        assertThat(underTest.editGeolocation(chatId, msgId, longitude, latitude, img))
            .isEqualTo(chatMessage)
    }

    @Test
    fun `test that original path is added to the cache`() {
        val newId = NodeId(2L)
        val newPath = "someInterestingPath/image.jpg"
        whenever(originalPathCache.get()).thenReturn(emptyMap())
        underTest.cacheOriginalPathForNode(newId, newPath)
        verify(originalPathCache).set(eq(mapOf(newId to newPath)))
    }

    @Test
    fun `test that original path is added to the cache when it's not empty`() {
        val originalId = NodeId(1L)
        val originalPath = "originalPath/video.mp4"
        val previousCache = mapOf(originalId to originalPath)
        val newId = NodeId(2L)
        val newPath = "someInterestingPath/image.jpg"
        whenever(originalPathCache.get()).thenReturn(previousCache)
        underTest.cacheOriginalPathForNode(newId, newPath)
        verify(originalPathCache).set(eq(previousCache + (newId to newPath)))
    }

    @Test
    internal fun `test that truncate messages calls the function on the message gateway`() =
        runTest {
            val expectedChatId = 5432L
            val truncateTimestamp = 23456L
            underTest.truncateMessages(expectedChatId, truncateTimestamp)

            verify(chatStorageGateway).truncateMessages(expectedChatId, truncateTimestamp)
        }
}