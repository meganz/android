package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MegaNodeFromChatMessageMapperTest {

    private lateinit var underTest: MegaNodeFromChatMessageMapper

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()

    private val chatId = 1L
    private val messageId = 2L

    @BeforeAll
    fun setup() {
        underTest = MegaNodeFromChatMessageMapper(
            megaChatApiGateway = megaChatApiGateway,
            megaApiGateway = megaApiGateway
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        megaApiGateway,
        megaChatApiGateway,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test basic functionality")
    inner class BasicFunctionality {

        private val megaNode = mock<MegaNode>()
        private val megaChatMessage = mock<MegaChatMessage>()
        private val megaNodeList = mock<MegaNodeList>()

        @BeforeEach
        fun resetMocks() = reset(megaNode, megaChatMessage, megaNodeList)

        @Test
        fun `test that node is returned when getMessage succeeds`() = runTest {
            whenever(megaNodeList.get(0)).thenReturn(megaNode)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
            whenever(megaChatApiGateway.getMessage(chatId, messageId))
                .thenReturn(megaChatMessage)
            whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(null)

            val result = underTest(chatId, messageId)

            assertThat(result).isEqualTo(megaNode)
        }

        @Test
        fun `test that node is returned when getMessageFromNodeHistory succeeds as fallback`() =
            runTest {
                whenever(megaNodeList.get(0)).thenReturn(megaNode)
                whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
                whenever(megaChatApiGateway.getMessage(chatId, messageId)).thenReturn(null)
                whenever(megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId))
                    .thenReturn(megaChatMessage)
                whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(null)

                val result = underTest(chatId, messageId)

                assertThat(result).isEqualTo(megaNode)
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test chat room preview authorization")
    inner class ChatRoomPreview {

        private val megaNode = mock<MegaNode>()
        private val authorizedNode = mock<MegaNode>()
        private val megaChatMessage = mock<MegaChatMessage>()
        private val megaNodeList = mock<MegaNodeList>()
        private val authorizationToken = "test-token"

        @BeforeEach
        fun resetMocks() = reset(megaNode, authorizedNode, megaChatMessage, megaNodeList)

        @Test
        fun `test that authorized node is returned when chat room is preview`() = runTest {
            val chatRoom = mock<MegaChatRoom> {
                on { isPreview }.thenReturn(true)
                on { authorizationToken }.thenReturn(authorizationToken)
            }

            whenever(megaNodeList.get(0)).thenReturn(megaNode)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
            whenever(megaChatApiGateway.getMessage(chatId, messageId))
                .thenReturn(megaChatMessage)
            whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(chatRoom)
            whenever(megaApiGateway.authorizeChatNode(megaNode, authorizationToken))
                .thenReturn(authorizedNode)

            val result = underTest(chatId, messageId)

            assertThat(result).isEqualTo(authorizedNode)
        }

        @Test
        fun `test that original node is returned when chat room is not preview`() = runTest {
            val chatRoom = mock<MegaChatRoom> {
                on { isPreview }.thenReturn(false)
            }

            whenever(megaNodeList.get(0)).thenReturn(megaNode)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
            whenever(megaChatApiGateway.getMessage(chatId, messageId))
                .thenReturn(megaChatMessage)
            whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(chatRoom)

            val result = underTest(chatId, messageId)

            assertThat(result).isEqualTo(megaNode)
        }

        @Test
        fun `test that original node is returned when chat room is null`() = runTest {
            whenever(megaNodeList.get(0)).thenReturn(megaNode)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
            whenever(megaChatApiGateway.getMessage(chatId, messageId))
                .thenReturn(megaChatMessage)
            whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(null)

            val result = underTest(chatId, messageId)

            assertThat(result).isEqualTo(megaNode)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test messageIndex parameter")
    inner class MessageIndex {

        private val megaNode0 = mock<MegaNode>()
        private val megaNode1 = mock<MegaNode>()
        private val megaChatMessage = mock<MegaChatMessage>()
        private val megaNodeList = mock<MegaNodeList>()

        @BeforeEach
        fun resetMocks() = reset(megaNode0, megaNode1, megaChatMessage, megaNodeList)

        @Test
        fun `test that correct node is returned for messageIndex 0`() = runTest {
            whenever(megaNodeList.get(0)).thenReturn(megaNode0)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
            whenever(megaChatApiGateway.getMessage(chatId, messageId))
                .thenReturn(megaChatMessage)
            whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(null)

            val result = underTest(chatId, messageId, 0)

            assertThat(result).isEqualTo(megaNode0)
        }

        @Test
        fun `test that correct node is returned for messageIndex 1`() = runTest {
            whenever(megaNodeList.get(1)).thenReturn(megaNode1)
            whenever(megaChatMessage.megaNodeList).thenReturn(megaNodeList)
            whenever(megaChatApiGateway.getMessage(chatId, messageId))
                .thenReturn(megaChatMessage)
            whenever(megaChatApiGateway.getChatRoom(chatId)).thenReturn(null)

            val result = underTest(chatId, messageId, 1)

            assertThat(result).isEqualTo(megaNode1)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @DisplayName("Test null and edge cases")
    inner class NullAndEdgeCases {

        @Test
        fun `test that null is returned when both getMessage and getMessageFromNodeHistory return null`() =
            runTest {
                whenever(megaChatApiGateway.getMessage(chatId, messageId)).thenReturn(null)
                whenever(megaChatApiGateway.getMessageFromNodeHistory(chatId, messageId))
                    .thenReturn(null)

                val result = underTest(chatId, messageId)

                assertThat(result).isNull()
            }

        @Test
        fun `test that null is returned when message is found but megaNodeList is empty`() =
            runTest {
                val megaChatMessage = mock<MegaChatMessage>()
                val emptyNodeList = mock<MegaNodeList> {
                    on { get(0) }.thenReturn(null)
                }

                whenever(megaChatMessage.megaNodeList).thenReturn(emptyNodeList)
                whenever(megaChatApiGateway.getMessage(chatId, messageId))
                    .thenReturn(megaChatMessage)

                val result = underTest(chatId, messageId)

                assertThat(result).isNull()
            }
    }
}
