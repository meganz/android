package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaNodeMapperTest {

    private lateinit var underTest: MegaNodeMapper

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()

    @BeforeEach
    fun setup() {
        underTest = MegaNodeMapper(
            megaApiGateway,
            megaChatApiGateway,
            megaApiFolderGateway,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        megaApiGateway,
        megaChatApiGateway,
        megaApiFolderGateway,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test mapping chat files")
    inner class ChatFile {

        private val megaNode = mock<MegaNode>()
        private val megaChatMessage = mock<MegaChatMessage>()
        private val megaNodeList = mock<MegaNodeList>()
        private val chatFile = mock<ChatDefaultFile>()

        @BeforeEach
        fun resetMocks() = reset(megaNode, megaChatMessage, megaNodeList, chatFile)

        private fun commonSetup() {
            whenever(chatFile.chatId).thenReturn(CHAT_ID)
            whenever(chatFile.messageId).thenReturn(MESSAGE_ID)
            whenever(chatFile.messageIndex).thenReturn(0)
            whenever(megaNodeList.get(0)).thenReturn(megaNode)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
        }

        @Test
        fun `test that the correct chat node from gateway is returned`() =
            runTest {
                commonSetup()
                whenever(megaChatApiGateway.getMessage(CHAT_ID, MESSAGE_ID))
                    .thenReturn(megaChatMessage)
                assertThat(underTest(chatFile)).isEqualTo(megaNode)
            }

        @Test
        fun `test that chat node history is returned as a fallback when node is not found`() =
            runTest {
                commonSetup()
                whenever(megaChatApiGateway.getMessage(CHAT_ID, MESSAGE_ID)).thenReturn(null)
                whenever(megaChatApiGateway.getMessageFromNodeHistory(CHAT_ID, MESSAGE_ID))
                    .thenReturn(megaChatMessage)
                assertThat(underTest(chatFile)).isEqualTo(megaNode)
            }

        @Test
        fun `test that chat node is authorized if is in chat preview`() = runTest {
            commonSetup()
            whenever(megaChatApiGateway.getMessage(CHAT_ID, MESSAGE_ID)).thenReturn(
                megaChatMessage
            )
            val megaNodeAuthorized = mock<MegaNode>()
            val authorizationToken = "token"
            val chat = mock<MegaChatRoom> {
                on { isPreview }.thenReturn(true)
                on { this.authorizationToken }.thenReturn(authorizationToken)
            }
            whenever(megaChatApiGateway.getChatRoom(CHAT_ID)).thenReturn(chat)
            whenever(megaApiGateway.authorizeChatNode(megaNode, authorizationToken))
                .thenReturn(megaNodeAuthorized)
            assertThat(underTest(chatFile)).isEqualTo(megaNodeAuthorized)
            verify(megaApiGateway).authorizeChatNode(megaNode, authorizationToken)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test mapping public folder links")
    inner class PublicLinksFolder {

        @Test
        fun `test that the authorized node from gateway is returned`() =
            runTest {
                val handle = 23L
                val nodeId = NodeId(handle)
                val publicLinkFolder = mock<PublicLinkFolder> {
                    on { this.id }.thenReturn(nodeId)
                }
                val megaNode = mock<MegaNode>()
                val authorizedNode = mock<MegaNode>()

                whenever(megaApiFolderGateway.getMegaNodeByHandle(handle)).thenReturn(megaNode)
                whenever(megaApiFolderGateway.authorizeNode(megaNode)).thenReturn(authorizedNode)
                assertThat(underTest(publicLinkFolder)).isEqualTo(authorizedNode)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test mapping public file links")
    inner class PublicLinksFile {
        @Test
        fun `test that null is returned when the node has no serialized data`() = runTest {
            val typedFileNode = mock<TypedFileNode> {
                on { this.serializedData }.thenReturn(null)
            }
            val publicLinkFile = mock<PublicLinkFile> {
                on { this.node }.thenReturn(typedFileNode)
            }
            assertThat(underTest(publicLinkFile)).isNull()
            verifyNoInteractions(megaApiGateway)
        }

        @Test
        fun `test that the correct node from gateway is returned`() = runTest {
            val megaNode = mock<MegaNode>()
            val typedFileNode = mock<TypedFileNode> {
                on { this.serializedData }.thenReturn(SERIALIZED_DATA)
            }
            val publicLinkFile = mock<PublicLinkFile> {
                on { this.node }.thenReturn(typedFileNode)
            }
            whenever(megaApiGateway.unSerializeNode(SERIALIZED_DATA)).thenReturn(megaNode)
            assertThat(underTest(publicLinkFile)).isEqualTo(megaNode)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test mapping ordinary nodes")
    inner class OrdinaryNodes {
        @Test
        fun `test that the correct file node from gateway is returned`() = runTest {
            val nodeId = NodeId(NODE_ID)
            val typedFileNode = mock<TypedFileNode> {
                on { id }.thenReturn(nodeId)
            }
            val megaNode = mock<MegaNode>()
            whenever(megaApiGateway.getMegaNodeByHandle(NODE_ID)).thenReturn(megaNode)
            assertThat(underTest(typedFileNode)).isEqualTo(megaNode)
        }

        @Test
        fun `test that the correct folder node from gateway is returned`() = runTest {
            val nodeId = NodeId(NODE_ID)
            val typedFolderNode = mock<TypedFolderNode> {
                on { id }.thenReturn(nodeId)
            }
            val megaNode = mock<MegaNode>()
            whenever(megaApiGateway.getMegaNodeByHandle(NODE_ID)).thenReturn(megaNode)
            assertThat(underTest(typedFolderNode)).isEqualTo(megaNode)
        }
    }

    companion object {
        private const val NODE_ID = 1L
        private const val CHAT_ID = 11L
        private const val MESSAGE_ID = 22L
        private const val SERIALIZED_DATA = "serializedData"
    }
}