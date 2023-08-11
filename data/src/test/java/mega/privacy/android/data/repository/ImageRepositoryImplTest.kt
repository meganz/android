package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.gateway.preferences.FileManagementPreferencesGateway
import mega.privacy.android.data.mapper.node.ImageNodeMapper
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.ImageRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ImageRepositoryImplTest {
    private lateinit var underTest: ImageRepository

    private val context: Context = mock()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val cacheGateway = mock<CacheGateway>()
    private val fileManagementPreferencesGateway = mock<FileManagementPreferencesGateway>()
    private val fileGateway = mock<FileGateway>()
    private val imageNodeMapper = mock<ImageNodeMapper>()

    private val chatRoomId = 1L
    private val chatMessageId = 1L
    private val authToken = "abc"
    private val nodeFileLink = "abc"

    private val megaNode = mock<MegaNode>()
    private val imageNode = mock<ImageNode>()
    private val nodeList = mock<MegaNodeList> {
        on { size() }.thenReturn(10)
        on { get(any()) }.thenReturn(megaNode)
    }
    private val chatMessage = mock<MegaChatMessage> {
        on { megaNodeList }.thenReturn(nodeList)
    }

    @BeforeAll
    fun setUp() {
        underTest = ImageRepositoryImpl(
            context = context,
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheGateway = cacheGateway,
            fileManagementPreferencesGateway = fileManagementPreferencesGateway,
            fileGateway = fileGateway,
            imageNodeMapper = imageNodeMapper,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            megaApiGateway,
            megaApiFolderGateway,
            megaChatApiGateway,
            cacheGateway,
            fileManagementPreferencesGateway,
            imageNodeMapper
        )
    }

    @Test
    fun `test that getImageNodeByHandle throws IllegalArgumentException when node is not found`() {
        runTest {
            val handle = 1L
            whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(null)
            assertThrows<IllegalArgumentException> {
                underTest.getImageNodeByHandle(
                    handle = handle
                )
            }
        }
    }

    @Test
    fun `test that getImageNodeByHandle retrieves mega node for folder link when node is not found by handle`() {
        runTest {
            val handle = 1L
            whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(null)
            whenever(megaApiFolderGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
            whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(megaNode)
            whenever(imageNodeMapper.invoke(any(), any(), any())).thenReturn(imageNode)
            val result = underTest.getImageNodeByHandle(
                handle = handle
            )
            assertThat(result).isEqualTo(imageNode)
        }
    }

    @Test
    fun `test that getImageNodeByHandle invokes imageNodeMapper when a valid megaNode is found`() {
        runTest {
            val handle = 1L
            whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
            underTest.getImageNodeByHandle(
                handle = handle
            )
            verify(imageNodeMapper).invoke(any(), any(), any())
        }
    }

    @Test
    fun `test that getImageNodeForPublicLink throws IllegalArgumentException when key for PublicNode is invalid`() {
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(true)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
                (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            assertThrows<IllegalArgumentException> {
                underTest.getImageNodeForPublicLink(nodeFileLink = nodeFileLink)
            }
        }
    }

    @Test
    fun `test that getImageNodeForPublicLink throws MegaException when api returns error`() {
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_EINTERNAL)
            }

            whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
                (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            assertThrows<MegaException> {
                underTest.getImageNodeForPublicLink(nodeFileLink = nodeFileLink)
            }
        }
    }

    @Test
    fun `test that getImageNodeForPublicLink invokes imageNodeMapper when a valid megaNode is found`() {
        runTest {
            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest> {
                on { flag }.thenReturn(false)
                on { publicMegaNode }.thenReturn(megaNode)
            }
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(megaApiGateway.getPublicNode(any(), any())).thenAnswer {
                (it.arguments[1] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }
            underTest.getImageNodeForPublicLink(nodeFileLink)
            verify(imageNodeMapper).invoke(any(), any(), any())
        }
    }

    @Test
    fun `test that getImageNodeForChatMessage throws IllegalArgumentException when ChatNode returned is null`() {
        runTest {
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(null)
            whenever(
                megaChatApiGateway.getMessageFromNodeHistory(
                    chatRoomId,
                    chatMessageId
                )
            ).thenReturn(null)
            assertThrows<IllegalArgumentException> {
                underTest.getImageNodeForChatMessage(
                    chatRoomId = chatRoomId,
                    chatMessageId = chatMessageId
                )
            }
        }
    }


    @Test
    fun `test that getImageNodeForChatMessage invokes getChatRoom when getting chatMessage from getMessage`() {
        runTest {
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(
                chatMessage
            )

            underTest.getImageNodeForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId
            )

            verify(megaChatApiGateway).getChatRoom(chatRoomId)
        }
    }

    @Test
    fun `test that getImageNodeForChatMessage invokes getChatRoom when getting chatMessage from getMessageFromNodeHistory`() {
        runTest {
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(null)
            whenever(
                megaChatApiGateway.getMessageFromNodeHistory(
                    chatRoomId,
                    chatMessageId
                )
            ).thenReturn(chatMessage)

            underTest.getImageNodeForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId
            )

            verify(megaChatApiGateway).getChatRoom(chatRoomId)
        }
    }

    @Test
    fun `test that getImageNodeForChatMessage invokes authorizeChatNode if chatRoom isPreview is true`() {
        runTest {
            val chatRoom = mock<MegaChatRoom> {
                on { isPreview }.thenReturn(true)
                on { authorizationToken }.thenReturn(authToken)
            }

            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(
                chatMessage
            )
            whenever(megaChatApiGateway.getChatRoom(chatRoomId)).thenReturn(chatRoom)
            whenever(megaApiGateway.authorizeChatNode(megaNode, authToken)).thenReturn(megaNode)

            underTest.getImageNodeForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId
            )

            verify(megaApiGateway).authorizeChatNode(megaNode, authToken)
        }
    }

    @Test
    fun `test that getImageNodeForChatMessage does not invoke authorizeChatNode if chatRoom isPreview is false`() {
        runTest {
            val chatRoom = mock<MegaChatRoom> {
                on { isPreview }.thenReturn(false)
                on { authorizationToken }.thenReturn(authToken)
            }
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(
                chatMessage
            )
            whenever(megaChatApiGateway.getChatRoom(chatRoomId)).thenReturn(chatRoom)
            whenever(megaApiGateway.authorizeChatNode(megaNode, authToken)).thenReturn(megaNode)

            underTest.getImageNodeForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId
            )

            verify(megaApiGateway, never()).authorizeChatNode(megaNode, authToken)
        }
    }

    @Test
    fun `test that getImageNodeForChatMessage invokes imageNodeMapper when a valid megaNode is found`() {
        runTest {
            val chatRoom = mock<MegaChatRoom> {
                on { isPreview }.thenReturn(true)
                on { authorizationToken }.thenReturn(authToken)
            }
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(
                chatMessage
            )
            whenever(megaChatApiGateway.getChatRoom(chatRoomId)).thenReturn(chatRoom)
            whenever(megaApiGateway.authorizeChatNode(megaNode, authToken)).thenReturn(megaNode)

            underTest.getImageNodeForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId
            )
            verify(imageNodeMapper).invoke(any(), any(), any())
        }
    }
}
