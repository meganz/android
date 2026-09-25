package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.entity.ChatRequestType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateChatPermissionsUseCaseTest {

    private lateinit var underTest: UpdateChatPermissionsUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = UpdateChatPermissionsUseCase(
            chatRepository = chatRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }

    @Test
    fun `test that the chat permission is updated with the correct arguments`() = runTest {
        val chatId = 123L
        val nodeId = NodeId(longValue = 321L)
        val permission = ChatRoomPermission.Standard

        underTest(chatId = chatId, nodeId = nodeId, permission = permission)

        verify(chatRepository).updateChatPermissions(
            chatId = chatId,
            nodeId = nodeId,
            permission = permission
        )
    }

    @Test
    fun `test that the correct chat request is returned`() = runTest {
        val chatId = 123L
        val nodeId = NodeId(longValue = 321L)
        val permission = ChatRoomPermission.Standard
        val chatRequest = ChatRequest(
            type = ChatRequestType.LoadPreview,
            requestString = null,
            tag = 0,
            number = 0,
            numRetry = 0,
            flag = false,
            peersList = null,
            chatHandle = chatId,
            userHandle = nodeId.longValue,
            privilege = 0,
            text = null,
            link = null,
            peersListByChatHandle = null,
            handleList = null,
            paramType = null
        )
        whenever(
            chatRepository.updateChatPermissions(
                chatId = chatId,
                nodeId = nodeId,
                permission = permission
            )
        ) doReturn chatRequest

        val actual = underTest(chatId = chatId, nodeId = nodeId, permission = permission)

        assertThat(actual).isEqualTo(chatRequest)
    }
}
