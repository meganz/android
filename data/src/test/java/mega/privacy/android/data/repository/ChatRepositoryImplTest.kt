package mega.privacy.android.data.repository

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.R
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.database.dao.ChatPendingChangesDao
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatConnectionStatusMapper
import mega.privacy.android.data.mapper.chat.ChatHistoryLoadStatusMapper
import mega.privacy.android.data.mapper.chat.ChatInitStateMapper
import mega.privacy.android.data.mapper.chat.ChatListItemMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.mapper.chat.ChatPreviewMapper
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.data.mapper.chat.ChatRoomMapper
import mega.privacy.android.data.mapper.chat.ChatRoomPendingChangesModelMapper
import mega.privacy.android.data.mapper.chat.CombinedChatRoomMapper
import mega.privacy.android.data.mapper.chat.ConnectionStateMapper
import mega.privacy.android.data.mapper.chat.MegaChatPeerListMapper
import mega.privacy.android.data.mapper.notification.ChatMessageNotificationBehaviourMapper
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.chat.NonContactInfo
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatPendingChanges
import mega.privacy.android.domain.entity.chat.ChatPreview
import mega.privacy.android.domain.entity.chat.ConnectionState
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ChatRepository
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNotificationListenerInterface
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaUser
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatRepositoryImplTest {

    private lateinit var underTest: ChatRepository

    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val chatRequestMapper = mock<ChatRequestMapper>()
    private val localStorageGateway = mock<MegaLocalStorageGateway>()
    private val chatRoomMapper = mock<ChatRoomMapper>()
    private val combinedChatRoomMapper = mock<CombinedChatRoomMapper>()
    private val chatListItemMapper = mock<ChatListItemMapper>()
    private val sharingScope = mock<CoroutineScope>()
    private val megaChatPeerListMapper = mock<MegaChatPeerListMapper>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val chatConnectionStatusMapper = ChatConnectionStatusMapper()
    private val connectionStateMapper = ConnectionStateMapper()
    private val chatMessageMapper = mock<ChatMessageMapper>()
    private val chatInitStateMapper = mock<ChatInitStateMapper>()
    private val chatMessageNotificationBehaviourMapper = ChatMessageNotificationBehaviourMapper()
    private val chatId = Random.nextLong()
    private val userHandle = Random.nextLong()
    private val userEmail = "test@email.com"
    private val chatTitle = "ChatTitle"
    private val chatRoom = mock<MegaChatRoom>()
    private val megaErrorSuccess = mock<MegaError> {
        on { errorCode }.thenReturn(MegaError.API_OK)
    }
    private val megaChatErrorSuccess = mock<MegaChatError> {
        on { errorCode }.thenReturn(MegaChatError.ERROR_OK)
    }
    private val appEventGateway = mock<AppEventGateway>()
    private val chatHistoryLoadStatusMapper = mock<ChatHistoryLoadStatusMapper>()
    private val chatPreviewMapper = mock<ChatPreviewMapper>()
    private val databaseHandler = mock<DatabaseHandler>()
    private val megaLocalRoomGateway = mock<MegaLocalRoomGateway>()
    private val chatRoomPendingChangesModelMapper = mock<ChatRoomPendingChangesModelMapper>()
    private val chatPendingChangesDao = mock<ChatPendingChangesDao>()
    private val context = mock<Context>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        underTest = ChatRepositoryImpl(
            megaChatApiGateway = megaChatApiGateway,
            megaApiGateway = megaApiGateway,
            chatRequestMapper = chatRequestMapper,
            localStorageGateway = localStorageGateway,
            chatRoomMapper = chatRoomMapper,
            combinedChatRoomMapper = combinedChatRoomMapper,
            chatListItemMapper = chatListItemMapper,
            chatMessageNotificationBehaviourMapper = chatMessageNotificationBehaviourMapper,
            sharingScope = sharingScope,
            ioDispatcher = testDispatcher,
            megaChatPeerListMapper = megaChatPeerListMapper,
            connectionStateMapper = connectionStateMapper,
            chatConnectionStatusMapper = chatConnectionStatusMapper,
            chatMessageMapper = chatMessageMapper,
            appEventGateway = appEventGateway,
            chatHistoryLoadStatusMapper = chatHistoryLoadStatusMapper,
            chatInitStateMapper = chatInitStateMapper,
            pendingMessageListMapper = mock(),
            chatPreviewMapper = chatPreviewMapper,
            databaseHandler = databaseHandler,
            megaLocalRoomGateway = megaLocalRoomGateway,
            chatStorageGateway = mock(),
            typedMessagePagingSourceMapper = mock(),
            typedMessageEntityMapper = mock(),
            messagePagingInfoMapper = mock(),
            richPreviewEntityMapper = mock(),
            giphyEntityMapper = mock(),
            chatGeolocationEntityMapper = mock(),
            chatNodeEntityListMapper = mock(),
            context = context,
        )

        whenever(chatRoom.chatId).thenReturn(chatId)
        whenever(chatRoom.title).thenReturn(chatTitle)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        Mockito.clearInvocations(
            megaChatApiGateway,
            megaApiGateway,
            chatRequestMapper,
            localStorageGateway,
        )

        reset(
            databaseHandler,
            megaChatApiGateway,
            megaLocalRoomGateway,
            chatMessageMapper,
        )
    }

    @Test
    fun `test that getChatRoom invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.getChatRoom(any())).thenReturn(chatRoom)

            underTest.getChatRoom(chatId)

            verify(megaChatApiGateway).getChatRoom(chatId)
            verify(chatRoomMapper).invoke(chatRoom)
        }

    @Test
    fun `test that setOpenInvite invokes the right methods`() =
        runTest {
            val isOpenInvite = Random.nextBoolean()

            whenever(chatRoom.isOpenInvite).thenReturn(isOpenInvite)
            whenever(megaChatApiGateway.getChatRoom(any())).thenReturn(chatRoom)
            whenever(megaChatApiGateway.setOpenInvite(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.setOpenInvite(chatId)

            verify(megaChatApiGateway).getChatRoom(chatId)
            verify(megaChatApiGateway).setOpenInvite(eq(chatId), eq(!isOpenInvite), any())
        }

    @Test
    fun `test that leaveChat invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.leaveChat(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.leaveChat(chatId)

            verify(megaChatApiGateway).leaveChat(eq(chatId), any())
            verify(chatRequestMapper).invoke(any())
        }

    @Test
    fun `test that getMeetingChatRooms invokes the right methods`() =
        runTest {
            val chatRoomList = listOf(chatRoom)
            val chatListItem = mock<MegaChatListItem>()

            whenever(megaChatApiGateway.getMeetingChatRooms()).thenReturn(chatRoomList)
            whenever(megaChatApiGateway.getChatListItem(any())).thenReturn(chatListItem)

            underTest.getMeetingChatRooms()

            verify(megaChatApiGateway, times(chatRoomList.size)).getChatListItem(eq(chatId))
            verify(combinedChatRoomMapper, times(chatRoomList.size)).invoke(chatRoom, chatListItem)
        }

    @Test
    fun `test that getCombinedChatRoom invokes the right methods`() =
        runTest {
            val chatListItem = mock<MegaChatListItem>()

            whenever(megaChatApiGateway.getChatRoom(any())).thenReturn(chatRoom)
            whenever(megaChatApiGateway.getChatListItem(any())).thenReturn(chatListItem)

            underTest.getCombinedChatRoom(chatId)

            verify(megaChatApiGateway).getChatRoom(chatId)
            verify(megaChatApiGateway).getChatListItem(chatId)
            verify(combinedChatRoomMapper).invoke(chatRoom, chatListItem)
        }

    @Test
    fun `test that inviteToChat invokes the right methods`() =
        runTest {
            val emails = listOf(userEmail)
            val megaUser = mock<MegaUser>()

            whenever(megaUser.handle).thenReturn(userHandle)
            whenever(megaApiGateway.getContact(any())).thenReturn(megaUser)

            underTest.inviteToChat(chatId, emails)

            verify(megaApiGateway, times(emails.size)).getContact(emails.first())
            verify(megaChatApiGateway, times(emails.size)).inviteToChat(
                eq(chatId),
                eq(userHandle),
                eq(null)
            )
        }

    @Test
    fun `test that setPublicChatToPrivate invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.setPublicChatToPrivate(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.setPublicChatToPrivate(chatId)

            verify(megaChatApiGateway).setPublicChatToPrivate(eq(chatId), any())
            verify(chatRequestMapper).invoke(any())
        }

    @Test
    fun `test that createChatLink invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.createChatLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.createChatLink(chatId)

            verify(megaChatApiGateway).createChatLink(eq(chatId), any())
            verify(chatRequestMapper).invoke(any())
        }

    @Test
    fun `test that removeChatLink invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.removeChatLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.removeChatLink(chatId)

            verify(megaChatApiGateway).removeChatLink(eq(chatId), any())
            verify(chatRequestMapper).invoke(any())
        }

    @Test
    fun `test that checkChatLink invokes the right methods`() =
        runTest {
            val chatLink = "https://mega.co.nz/sample"

            whenever(megaChatApiGateway.checkChatLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.checkChatLink(chatLink)

            verify(megaChatApiGateway).checkChatLink(eq(chatLink), any())
            verify(chatRequestMapper).invoke(any())
        }

    @Test
    fun `test that queryChatLink invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.queryChatLink(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.queryChatLink(chatId)

            verify(megaChatApiGateway).queryChatLink(eq(chatId), any())
            verify(chatRequestMapper).invoke(any())
        }

    @Test
    fun `test that inviteContact invokes the right methods`() =
        runTest {
            whenever(megaApiGateway.inviteContact(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaErrorSuccess,
                )
            }

            val result = underTest.inviteContact(userEmail)

            verify(megaApiGateway).inviteContact(eq(userEmail), any())
            assertThat(result).isEqualTo(InviteContactRequest.Sent)
        }

    @Test
    fun `test that updateChatPermissions invokes the right methods`() =
        runTest {
            val permissions = ChatRoomPermission.Standard to MegaChatRoom.PRIV_STANDARD

            whenever(
                megaChatApiGateway.updateChatPermissions(any(), any(), any(), any())
            ).thenAnswer {
                ((it.arguments[3]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.updateChatPermissions(chatId, userHandle, permissions.first)

            verify(megaChatApiGateway).updateChatPermissions(
                eq(chatId),
                eq(userHandle),
                eq(permissions.second),
                any()
            )
        }

    @Test
    fun `test that removeFromChat invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.removeFromChat(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.removeFromChat(chatId, userHandle)

            verify(megaChatApiGateway).removeFromChat(eq(chatId), eq(userHandle), any())
            verify(chatRequestMapper).invoke(any())
        }

    @Test
    fun `test that isChatLastMessageGeolocation invokes the right methods`() =
        runTest {
            val chatMessage = mock<MegaChatMessage>()
            val chatListItem = mock<MegaChatListItem>()
            val chatMessageMeta = mock<MegaChatContainsMeta>()
            val containsMeta = Random.nextBoolean()
            val containsGeolocation = Random.nextBoolean()

            if (containsMeta) {
                whenever(chatListItem.lastMessageType).thenReturn(MegaChatMessage.TYPE_CONTAINS_META)
            }
            if (containsGeolocation) {
                whenever(chatMessageMeta.type).thenReturn(MegaChatContainsMeta.CONTAINS_META_GEOLOCATION)
            }
            whenever(chatMessage.containsMeta).thenReturn(chatMessageMeta)
            whenever(megaChatApiGateway.getMessage(any(), any())).thenReturn(chatMessage)
            whenever(megaChatApiGateway.getChatListItem(any())).thenReturn(chatListItem)

            val result = underTest.isChatLastMessageGeolocation(chatId)

            verify(megaChatApiGateway).getMessage(eq(chatId), any())
            verify(megaChatApiGateway).getChatListItem(chatId)
            assertThat(result).isEqualTo(containsMeta && containsGeolocation)
        }

    @Test
    fun `test that signalPresenceActivity invokes the right methods`() =
        runTest {
            whenever(megaChatApiGateway.signalPresenceActivity(any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.signalPresenceActivity()

            verify(megaChatApiGateway).signalPresenceActivity(any())
        }

    @Test
    fun `test that archiveChat invokes the right methods`() =
        runTest {
            val archive = Random.nextBoolean()

            whenever(megaChatApiGateway.archiveChat(any(), any(), any())).thenAnswer {
                ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.archiveChat(chatId, archive)

            verify(megaChatApiGateway).archiveChat(eq(chatId), eq(archive), any())
        }

    @Test
    fun `test that create creates new chatroom and returns the id of new chatroom`() = runTest {
        val megaChatRequest = mock<MegaChatRequest> {
            on { chatHandle }.thenReturn(chatId)
        }
        whenever(megaChatPeerListMapper.invoke(listOf(userHandle))).thenReturn(mock())
        whenever(
            megaChatApiGateway.createChat(any(), any(), any())
        ).thenAnswer {
            ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequest,
                megaChatErrorSuccess,
            )
        }
        val actual = underTest.createChat(isGroup = true, userHandles = listOf(userHandle))
        assertThat(actual).isEqualTo(chatId)
        verify(megaChatApiGateway).createChat(any(), any(), any())
    }

    @Test
    fun `test that get connection state returns ConnectionStatus`() = runTest {
        whenever(megaChatApiGateway.getConnectedState()).thenReturn(2)
        val actual = underTest.getConnectionState()
        assertThat(actual).isEqualTo(ConnectionState.Connected)
    }

    @Test
    fun `test that get chat connection state returns ChatConnectionState`() = runTest {
        whenever(megaChatApiGateway.getChatConnectionState(chatId = chatId)).thenReturn(3)
        val actual = underTest.getChatConnectionState(chatId = chatId)
        assertThat(actual).isEqualTo(ChatConnectionStatus.Online)
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
    fun `test that openChatPreview returns data correctly when call megaApi`() = runTest {
        val link = "link"
        val chatPreview = mock<ChatPreview>()
        whenever(megaChatApiGateway.openChatPreview(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaChatErrorSuccess,
            )
        }
        whenever(chatPreviewMapper(any(), any())).thenReturn(chatPreview)
        assertThat(underTest.openChatPreview(link)).isEqualTo(chatPreview)
    }


    @Test
    fun `test that first name returns correctly when calling from contact database`() = runTest {
        val handle = 123L
        val contact = Contact(
            email = "email",
            firstName = "firstName",
            lastName = "lastName",
            nickname = "nickname",
            userId = handle,
        )
        whenever(megaLocalRoomGateway.getContactByHandle(handle)).thenReturn(contact)
        assertThat(underTest.getParticipantFirstName(handle, false)).isEqualTo(contact.nickname)
    }

    @Test
    fun `test that first name returns correctly when calling from non-contact database`() =
        runTest {
            val handle = 123L
            val nonContact = NonContactInfo(
                handle.toString(),
                "fullName",
                "firstName",
                "lastName",
                "email",
            )
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(
                nonContact
            )
            assertThat(underTest.getParticipantFirstName(handle, false))
                .isEqualTo(nonContact.firstName)
        }

    @Test
    fun `test that first name returns correctly when calling from alias sdk cache`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserAliasFromCache(handle)).thenReturn("alias")
            assertThat(underTest.getParticipantFirstName(handle, false)).isEqualTo("alias")
        }

    @Test
    fun `test that first name returns correctly when calling from first name sdk cache`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserAliasFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserFirstnameFromCache(handle)).thenReturn("firstName")
            assertThat(underTest.getParticipantFirstName(handle, false)).isEqualTo("firstName")
        }

    @Test
    fun `test that first name returns correctly when calling from last name sdk cache`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserAliasFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserFirstnameFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserLastnameFromCache(handle)).thenReturn("lastName")
            assertThat(underTest.getParticipantFirstName(handle, false)).isEqualTo("lastName")
        }

    @Test
    fun `test that first name returns correctly when calling without contemplating email`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserAliasFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserFirstnameFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserLastnameFromCache(handle)).thenReturn(null)
            assertThat(underTest.getParticipantFirstName(handle, false)).isNull()
        }

    @Test
    fun `test that first name returns correctly when calling from last email sdk cache`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserAliasFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserFirstnameFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserLastnameFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserEmailFromCache(handle)).thenReturn("email")
            assertThat(underTest.getParticipantFirstName(handle, true)).isEqualTo("email")
        }

    @Test
    fun `test that first name returns correctly when calling from last email sdk cache and it is null`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserAliasFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserFirstnameFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserLastnameFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserEmailFromCache(handle)).thenReturn(null)
            assertThat(underTest.getParticipantFirstName(handle, true)).isNull()
        }

    @Test
    fun `test that full name returns correctly when calling from contact database`() = runTest {
        val handle = 123L
        val contact = Contact(
            email = "email",
            firstName = "firstName",
            lastName = "lastName",
            nickname = "nickname",
            userId = handle,
        )
        whenever(megaLocalRoomGateway.getContactByHandle(handle)).thenReturn(contact)
        assertThat(underTest.getParticipantFullName(handle)).isEqualTo(contact.fullName)
    }

    @Test
    fun `test that full name returns correctly when calling from non-contact database`() =
        runTest {
            val handle = 123L
            val nonContact = NonContactInfo(
                handle.toString(),
                "fullName",
                "firstName",
                "lastName",
                "email",
            )
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(
                nonContact
            )
            assertThat(underTest.getParticipantFullName(handle)).isEqualTo(nonContact.fullName)
        }

    @Test
    fun `test that full name returns correctly when calling from full name sdk cache`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserFullNameFromCache(handle)).thenReturn("fullName")
            assertThat(underTest.getParticipantFullName(handle)).isEqualTo("fullName")
        }

    @Test
    fun `test that full name returns correctly when calling from email sdk cache`() =
        runTest {
            val handle = 123L
            whenever(databaseHandler.findContactByHandle(handle)).thenReturn(null)
            whenever(databaseHandler.findNonContactByHandle(handle.toString())).thenReturn(null)
            whenever(megaChatApiGateway.getUserFullNameFromCache(handle)).thenReturn(null)
            whenever(megaChatApiGateway.getUserEmailFromCache(handle)).thenReturn("email")
            assertThat(underTest.getParticipantFullName(handle)).isEqualTo("email")
        }

    @Test
    fun `test that my full name returns correctly`() = runTest {
        val fullName = "fullName"
        whenever(megaChatApiGateway.getMyFullname()).thenReturn(fullName)
        assertThat(underTest.getMyFullName()).isEqualTo(fullName)
    }

    @Test
    fun `test that returns correctly when calling get my user handle`() = runTest {
        whenever(megaChatApiGateway.getMyUserHandle()).thenReturn(123L)
        assertThat(underTest.getMyUserHandle()).isEqualTo(123L)
    }

    @Test
    fun `test that enable audio level monitor invokes megaApi correctly`() =
        runTest {
            val enable = true
            whenever(megaChatApiGateway.enableAudioLevelMonitor(enable, chatId)).thenReturn(Unit)
            underTest.enableAudioLevelMonitor(enable, chatId)
            verify(megaChatApiGateway).enableAudioLevelMonitor(enable, chatId)
            verifyNoMoreInteractions(megaChatApiGateway)
        }

    @Test
    fun `test that is audio level monitor enabled invokes megaApi correctly`() =
        runTest {
            val enabled = true
            whenever(megaChatApiGateway.isAudioLevelMonitorEnabled(chatId)).thenReturn(enabled)
            assertThat(underTest.isAudioLevelMonitorEnabled(chatId)).isEqualTo(enabled)
            verify(megaChatApiGateway).isAudioLevelMonitorEnabled(chatId)
            verifyNoMoreInteractions(megaChatApiGateway)
        }

    @Test
    fun `test that clear chat history invokes megaChatApi and localStorageGateway if request finish with success`() =
        runTest {
            whenever(megaChatApiGateway.clearChatHistory(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.clearChatHistory(chatId)
            verify(megaChatApiGateway).clearChatHistory(eq(chatId), any())
            verify(localStorageGateway).removePendingMessageByChatId(chatId)
            verifyNoMoreInteractions(megaChatApiGateway)
            verifyNoMoreInteractions(localStorageGateway)
        }

    @Test
    fun `test that clear chat history invokes megaChatApi and do not invokes localStorageGateway if request finish with error`() =
        runTest {
            val error = mock<MegaChatError> {
                on { errorCode }.thenReturn(MegaChatError.ERROR_ACCESS)
            }
            whenever(megaChatApiGateway.clearChatHistory(any(), any())).thenAnswer {
                ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    error,
                )
            }

            assertThrows<MegaException> { underTest.clearChatHistory(chatId) }
            verify(megaChatApiGateway).clearChatHistory(eq(chatId), any())
            verifyNoMoreInteractions(megaChatApiGateway)
            verifyNoInteractions(localStorageGateway)
        }

    @Test
    fun `test that endChatCall returns correctly when megaApi returns the flags`() = runTest {
        val expectedResult = true
        val callId = 123L
        val megaChatRequest = mock<MegaChatRequest> { on { flag }.thenReturn(expectedResult) }
        val call = mock<MegaChatCall> { on { this.callId }.thenReturn(callId) }
        whenever(megaChatApiGateway.getChatCall(any())).thenReturn(call)
        whenever(megaChatApiGateway.endChatCall(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                megaChatRequest,
                megaChatErrorSuccess,
            )
        }
        val actual = underTest.endChatCall(chatId)
        assertThat(actual).isEqualTo(expectedResult)
    }

    @Test
    fun `test that is geolocation enabled returns true if request finish with success`() = runTest {

        whenever(megaApiGateway.isGeolocationEnabled(any())).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaErrorSuccess,
            )
        }
        assertThat(underTest.isGeolocationEnabled()).isTrue()
        verify(megaApiGateway).isGeolocationEnabled(any())
        verifyNoMoreInteractions(megaApiGateway)
    }

    @Test
    fun `test that is geolocation enabled returns false if request finish with error`() = runTest {
        val error = mock<MegaError> {
            on { errorCode }.thenReturn(MegaError.API_EACCESS)
        }
        whenever(megaApiGateway.isGeolocationEnabled(any())).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                error,
            )
        }
        assertThat(underTest.isGeolocationEnabled()).isFalse()
        verify(megaApiGateway).isGeolocationEnabled(any())
        verifyNoMoreInteractions(megaApiGateway)
    }

    @Test
    fun `test that enable geolocation invokes mega api`() = runTest {
        whenever(megaApiGateway.enableGeolocation(any())).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaErrorSuccess,
            )
        }
        underTest.enableGeolocation()
        verify(megaApiGateway).enableGeolocation(any())
        verifyNoMoreInteractions(megaApiGateway)
    }

    @Test
    fun `test that null messages are returned from monitor messages function`() = runTest {
        val chatMessage = mock<ChatMessage>()
        chatMessageMapper.stub { onBlocking { invoke(any()) }.thenReturn(chatMessage) }

        whenever(megaChatApiGateway.openChatRoom(any())).thenReturn(flow {
            emit(ChatRoomUpdate.OnMessageLoaded(mock()))
            emit(ChatRoomUpdate.OnMessageLoaded(null))
            awaitCancellation()
        })

        underTest.monitorOnMessageLoaded(123L).test {
            assertThat(awaitItem()).isNotNull()
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `test that send message invokes mega chat api`() = runTest {
        val chatId = 123L
        val message = "Message"
        whenever(megaChatApiGateway.sendMessage(chatId, message)).thenReturn(mock())
        underTest.sendMessage(chatId, message)
        verify(megaChatApiGateway).sendMessage(chatId, message)
    }

    @Test
    fun `test that set last handle invokes correctly`() = runTest {
        val chatId = 123L
        underTest.setLastPublicHandle(chatId)
        verify(localStorageGateway).setLastPublicHandle(chatId)
        verify(localStorageGateway).setLastPublicHandleTimeStamp()
    }

    @Test
    fun `test that auto join public chat invokes correctly`() = runTest {
        val chatId = 123L
        whenever(megaChatApiGateway.autojoinPublicChat(any(), any())).thenAnswer {
            ((it.arguments[1]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaChatErrorSuccess,
            )
        }
        underTest.autojoinPublicChat(chatId)
        verify(megaChatApiGateway).autojoinPublicChat(eq(chatId), any())
    }

    @Test
    fun `test that autorejoin public chat invokes correctly`() = runTest {
        val chatId = 123L
        val chatPublicHandle = 456L
        whenever(megaChatApiGateway.autorejoinPublicChat(any(), any(), any())).thenAnswer {
            ((it.arguments[2]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                mock(),
                mock(),
                megaChatErrorSuccess,
            )
        }
        underTest.autorejoinPublicChat(chatId, chatPublicHandle)
        verify(megaChatApiGateway).autorejoinPublicChat(eq(chatId), eq(chatPublicHandle), any())
    }

    @Test
    fun `test that should show rich warning returns correctly`() = runTest {
        val shouldShow = true
        val request = mock<MegaRequest> {
            on { flag }.thenReturn(shouldShow)
            on { number }.thenReturn(1L)
        }
        whenever(megaApiGateway.shouldShowRichLinkWarning(any())).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                request,
                megaErrorSuccess,
            )
        }
        assertThat(underTest.shouldShowRichLinkWarning()).isEqualTo(shouldShow)
        underTest.monitorRichLinkPreviewConfig().test {
            val item = awaitItem()
            assertThat(item.isShowRichLinkWarning).isEqualTo(shouldShow)
            assertThat(item.counterNotNowRichLinkWarning).isEqualTo(1)
        }
    }

    @Test
    fun `test that is rich link enabled returns correctly`() = runTest {
        val isEnabled = true
        val request = mock<MegaRequest> {
            on { flag }.thenReturn(isEnabled)
        }
        whenever(megaApiGateway.isRichPreviewsEnabled(any())).thenAnswer {
            ((it.arguments[0]) as OptionalMegaRequestListenerInterface).onRequestFinish(
                mock(),
                request,
                megaErrorSuccess,
            )
        }
        assertThat(underTest.isRichPreviewsEnabled()).isEqualTo(isEnabled)
        underTest.monitorRichLinkPreviewConfig().test {
            val item = awaitItem()
            assertThat(item.isRichLinkEnabled).isEqualTo(isEnabled)
        }
    }

    @Test
    fun `test that close chat preview invokes correctly`() = runTest {
        underTest.closeChatPreview(chatId)
        verify(megaChatApiGateway).closeChatPreview(chatId)
    }

    @Test
    fun `test that send geolocation invokes chat api`() = runTest {
        val chatId = 123L
        val longitude = 1.0F
        val latitude = 1.0F
        val image = "image"
        whenever(
            megaChatApiGateway.sendGeolocation(
                chatId = chatId,
                longitude = longitude,
                latitude = latitude,
                image = image
            )
        ).thenReturn(mock())
        underTest.sendGeolocation(
            chatId = chatId,
            longitude = longitude,
            latitude = latitude,
            image = image
        )
        verify(megaChatApiGateway).sendGeolocation(
            chatId = chatId,
            longitude = longitude,
            latitude = latitude,
            image = image
        )
    }

    @Test
    fun `test that set chat draft message invokes correctly`() = runTest {
        val chatId = 123L
        val message = "message"
        val draftMessage = "draftMessage"
        val model = ChatPendingChanges(chatId, message)
        whenever(megaLocalRoomGateway.monitorChatPendingChanges(chatId)).thenReturn(flowOf(model))
        underTest.setChatDraftMessage(chatId, draftMessage)
        verify(megaLocalRoomGateway).setChatPendingChanges(model.copy(draftMessage = draftMessage))
    }

    @Test
    fun `test that get chat room preferences returns correctly`() = runTest {
        val chatId = 123L
        val message = "message"
        val model = ChatPendingChanges(chatId, message)
        whenever(megaLocalRoomGateway.monitorChatPendingChanges(chatId)).thenReturn(flowOf(model))
        underTest.monitorChatPendingChanges(chatId).test {
            assertThat(awaitItem()).isEqualTo(model)
            awaitComplete()
        }
    }

    @Test
    fun `test that the default chat folder name is retrieved`() = runTest {
        val expected = "Default chat folder name"
        whenever(context.getString(R.string.my_chat_files_folder)).thenReturn(expected)
        val actual = underTest.getDefaultChatFolderName()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that add reaction invokes correctly`() = runTest {
        val chatId = 123L
        val msgId = 456L
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
    fun `test that null is returned from monitor chat notification if the message is null`() =
        runTest {
            megaChatApiGateway.stub {
                on { registerChatNotificationListener(any()) }.thenAnswer { invocation ->
                    val listener = invocation.arguments[0] as MegaChatNotificationListenerInterface
                    listener.onChatNotification(mock(), 1L, null)
                }
            }

            underTest.monitorChatMessages().test {
                assertThat(awaitItem()).isNull()
            }
        }

    @Test
    internal fun `test that chat message notification is returned from monitor chat notification if message is not null`() =
        runTest {
            val sdkMessage = mock<MegaChatMessage>()
            val chatMessage = mock<ChatMessage>()
            chatMessageMapper.stub { onBlocking { invoke(sdkMessage) }.thenReturn(chatMessage) }

            val chatId = 1L
            megaChatApiGateway.stub {
                on { registerChatNotificationListener(any()) }.thenAnswer { invocation ->
                    val listener = invocation.arguments[0] as MegaChatNotificationListenerInterface
                    listener.onChatNotification(mock(), chatId, sdkMessage)
                }
            }

            underTest.monitorChatMessages().test {
                val actual = awaitItem()
                assertThat(actual).isNotNull()
                assertThat(actual?.chatId).isEqualTo(chatId)
                assertThat(actual?.message).isEqualTo(chatMessage)
            }
        }
}
