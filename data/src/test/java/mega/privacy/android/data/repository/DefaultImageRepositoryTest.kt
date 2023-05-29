package mega.privacy.android.data.repository

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.constant.CacheFolderConstant
import mega.privacy.android.data.gateway.CacheFolderGateway
import mega.privacy.android.data.gateway.CacheGateway
import mega.privacy.android.data.gateway.FileGateway
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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultImageRepositoryTest {
    private lateinit var underTest: ImageRepository

    private val context: Context = mock()
    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val cacheGateway = mock<CacheGateway>()
    private val fileManagementPreferencesGateway = mock<FileManagementPreferencesGateway>()
    private val fileGateway = mock<FileGateway>()
    private val cacheFolderGateway = mock<CacheFolderGateway>()
    private val imageNodeMapper = mock<ImageNodeMapper>()

    private val cacheDir = File("cache")

    private val chatRoomId = 1L
    private val chatMessageId = 1L
    private val authToken = "abc"
    private val nodeFileLink = "abc"

    private val megaNode = mock<MegaNode>()
    private val nodeList = mock<MegaNodeList> {
        on { size() }.thenReturn(10)
        on { get(any()) }.thenReturn(megaNode)
    }
    private val chatMessage = mock<MegaChatMessage> {
        on { megaNodeList }.thenReturn(nodeList)
    }

    @Before
    fun setUp() {
        underTest = DefaultImageRepository(
            context = context,
            megaApiGateway = megaApiGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            cacheGateway = cacheGateway,
            fileManagementPreferencesGateway = fileManagementPreferencesGateway,
            fileGateway = fileGateway,
            cacheFolderGateway = cacheFolderGateway,
            imageNodeMapper = imageNodeMapper,
        )
    }

    @Test
    fun `test that get thumbnail from server returns successfully if no error is thrown`() {
        runTest {
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK)
            }

            whenever(megaApiGateway.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            val actual = underTest.getThumbnailFromServer(1L)
            assertThat(actual?.path).isEqualTo(expectedPath)
        }
    }

    @Test(expected = MegaException::class)
    fun `test that get thumbnail from server returns doesn't successfully`() {
        runTest {
            val thumbnailName = "test"
            val expectedPath =
                "${cacheDir.path}/${CacheFolderConstant.THUMBNAIL_FOLDER}/$thumbnailName"
            val thumbnail = File(expectedPath)

            whenever(megaNode.base64Handle).thenReturn(thumbnailName)
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(thumbnail)

            val api = mock<MegaApiJava>()
            val request = mock<MegaRequest>()
            val error = mock<MegaError> {
                on { errorCode }.thenReturn(MegaError.API_OK + 1)
            }

            whenever(megaApiGateway.getThumbnail(any(), any(), any())).thenAnswer {
                (it.arguments[2] as MegaRequestListenerInterface).onRequestFinish(
                    api,
                    request,
                    error
                )
            }

            underTest.getThumbnailFromServer(1L)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodeHandle throws exception if node is null`() {
        runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(null)
            underTest.getImageByNodeHandle(
                nodeHandle = 1L,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )
        }
    }


    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodeHandle throws exception if node is not file`() {
        runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(1L)).thenReturn(megaNode)
            whenever(megaNode.isFile).thenReturn(false)
            underTest.getImageByNodeHandle(
                nodeHandle = 1L,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodeHandle throws exception if full image file is null`() {
        runTest {
            whenever(megaApiGateway.getMegaNodeByHandle(any())).thenReturn(megaNode)
            whenever(cacheGateway.getCacheFile(any(), anyOrNull())).thenReturn(null)
            underTest.getImageByNodeHandle(
                nodeHandle = 1L,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodePublicLink throws exception if nodeFileLink is blank`() {
        runTest {
            underTest.getImageByNodePublicLink(
                nodeFileLink = "",
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageByNodePublicLink throws exception if key for PublicNode is invalid`() {
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

            underTest.getImageByNodePublicLink(
                nodeFileLink = nodeFileLink,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )
        }
    }


    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageForChatMessage throws exception if ChatNode returned is null`() {
        runTest {
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(null)
            whenever(
                megaChatApiGateway.getMessageFromNodeHistory(
                    chatRoomId,
                    chatMessageId
                )
            ).thenReturn(null)

            underTest.getImageForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageForChatMessage invokes getMessage methods from megaChatApiGateway`() {
        runTest {
            underTest.getImageForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )

            verify(megaChatApiGateway).getMessage(chatRoomId, chatMessageId)
            verify(megaChatApiGateway).getMessageFromNodeHistory(
                chatRoomId,
                chatMessageId
            )
        }
    }

    @Test
    fun `test that getImageForChatMessage invokes getChatRoom on getting chatMessage from getMessage`() {
        runTest {
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(
                chatMessage
            )

            underTest.getImageForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )

            verify(megaChatApiGateway).getChatRoom(chatRoomId)
        }
    }

    @Test
    fun `test that getImageForChatMessage invokes getChatRoom on getting chatMessage from getMessageFromNodeHistory`() {
        runTest {
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(null)
            whenever(
                megaChatApiGateway.getMessageFromNodeHistory(
                    chatRoomId,
                    chatMessageId
                )
            ).thenReturn(chatMessage)

            underTest.getImageForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )

            verify(megaChatApiGateway).getChatRoom(chatRoomId)
        }
    }

    @Test
    fun `test that getImageForChatMessage invokes authorizeChatNode if chatRoom isPreview is true`() {
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

            underTest.getImageForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )

            verify(megaApiGateway).authorizeChatNode(megaNode, authToken)
        }
    }

    @Test
    fun `test that getImageForChatMessage does not invoke authorizeChatNode if chatRoom isPreview is false`() {
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

            underTest.getImageForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId,
                fullSize = false,
                highPriority = false,
                isMeteredConnection = false,
                resetDownloads = {}
            )

            verify(megaApiGateway, never()).authorizeChatNode(megaNode, authToken)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageNodeByHandle throws IllegalArgumentException if node is not found`() {
        runTest {
            val handle = 1L
            whenever(megaApiGateway.getMegaNodeByHandle(handle)).thenReturn(null)
            underTest.getImageNodeByHandle(
                handle = handle
            )
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
            verify(imageNodeMapper).invoke(any(), any())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageNodeForPublicLink throws IllegalArgumentException if key for PublicNode is invalid`() {
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
            underTest.getImageNodeForPublicLink(nodeFileLink = nodeFileLink)
        }
    }

    @Test(expected = MegaException::class)
    fun `test that getImageNodeForPublicLink throws MegaException if api returns error`() {
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
            underTest.getImageNodeForPublicLink(nodeFileLink = nodeFileLink)
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
            verify(imageNodeMapper).invoke(any(), any())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test that getImageNodeForChatMessage throws IllegalArgumentException if ChatNode returned is null`() {
        runTest {
            whenever(megaChatApiGateway.getMessage(chatRoomId, chatMessageId)).thenReturn(null)
            whenever(
                megaChatApiGateway.getMessageFromNodeHistory(
                    chatRoomId,
                    chatMessageId
                )
            ).thenReturn(null)

            underTest.getImageNodeForChatMessage(
                chatRoomId = chatRoomId,
                chatMessageId = chatMessageId
            )
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
            verify(imageNodeMapper).invoke(any(), any())
        }
    }


}