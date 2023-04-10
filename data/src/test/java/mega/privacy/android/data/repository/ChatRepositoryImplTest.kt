package mega.privacy.android.data.repository

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.data.gateway.DeviceEventGateway
import mega.privacy.android.data.gateway.MegaLocalStorageGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaChatRequestListenerInterface
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.chat.ChatListItemMapper
import mega.privacy.android.data.mapper.chat.ChatRoomMapper
import mega.privacy.android.data.mapper.chat.CombinedChatRoomMapper
import mega.privacy.android.data.mapper.chat.ChatRequestMapper
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.repository.ChatRepository
import nz.mega.sdk.MegaChatContainsMeta
import nz.mega.sdk.MegaChatError
import nz.mega.sdk.MegaChatListItem
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaUser
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
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
    private val deviceEventGateway = mock<DeviceEventGateway>()
    private val testDispatcher = UnconfinedTestDispatcher()
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

    @Before
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
            sharingScope = sharingScope,
            ioDispatcher = testDispatcher,
            deviceEventGateway = deviceEventGateway,
        )

        whenever(chatRoom.chatId).thenReturn(chatId)
        whenever(chatRoom.title).thenReturn(chatTitle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
            val signalActivityRequired = Random.nextBoolean()

            whenever(megaChatApiGateway.isSignalActivityRequired()).thenReturn(
                signalActivityRequired
            )
            whenever(megaChatApiGateway.signalPresenceActivity(any())).thenAnswer {
                ((it.arguments[0]) as OptionalMegaChatRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    megaChatErrorSuccess,
                )
            }

            underTest.signalPresenceActivity()

            if (signalActivityRequired) {
                verify(megaChatApiGateway).signalPresenceActivity(any())
            } else {
                verify(megaChatApiGateway, never()).signalPresenceActivity(any())
            }
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
}
