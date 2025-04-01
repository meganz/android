package mega.privacy.android.app.presentation.bottomsheet

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartDownloadViewModelTest {

    private lateinit var underTest: StartDownloadViewModel

    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getPublicNodeFromSerializedDataUseCase =
        mock<GetPublicNodeFromSerializedDataUseCase>()
    private val getPublicChildNodeFromIdUseCase = mock<GetPublicChildNodeFromIdUseCase>()

    @BeforeAll
    fun setup() {
        initViewModel()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getChatFileUseCase,
            getNodeByIdUseCase,
            getPublicNodeFromSerializedDataUseCase,
            getPublicChildNodeFromIdUseCase,
        )
    }

    private fun initViewModel() {
        underTest = StartDownloadViewModel(
            getChatFileUseCase,
            getNodeByIdUseCase,
            getPublicNodeFromSerializedDataUseCase,
            getPublicChildNodeFromIdUseCase,
        )
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onDownloadClicked launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val node = mock<TypedFileNode>()
        underTest.onDownloadClicked(node = node, withStartMessage = withStartMessage)
        assertStartDownloadNode(node, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onSaveOfflineClicked launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val node = mock<TypedFileNode>()
        underTest.onSaveOfflineClicked(node = node, withStartMessage = withStartMessage)
        assertStartDownloadForOffline(node, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onDownloadClicked with id launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val nodeId = NodeId(1L)
        val node = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        underTest.onDownloadClicked(nodeId = nodeId, withStartMessage = withStartMessage)
        assertStartDownloadNode(node, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onDownloadClicked with serialized data launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val node = mock<PublicLinkFile>()
        val serializedData = "serialized data"
        whenever(getPublicNodeFromSerializedDataUseCase(serializedData)).thenReturn(node)
        underTest.onDownloadClicked(
            serializedData = serializedData,
            withStartMessage = withStartMessage
        )
        assertStartDownloadNode(node, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onFolderLinkChildNodeDownloadClicked  launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val node = mock<PublicLinkFile>()
        val id = NodeId(1L)
        whenever(getPublicChildNodeFromIdUseCase(id)).thenReturn(node)
        underTest.onFolderLinkChildNodeDownloadClicked(
            nodeId = id,
            withStartMessage = withStartMessage
        )
        assertStartDownloadNode(node, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onSaveOfflineClicked with id launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val nodeId = NodeId(1L)
        val node = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        underTest.onSaveOfflineClicked(nodeId = nodeId, withStartMessage = withStartMessage)
        assertStartDownloadForOffline(node, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onDownloadClicked for chat file launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val chatId = 11L
        val messageId = 22L
        val chatFile = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
        underTest.onDownloadClicked(
            chatId = chatId,
            messageId = messageId,
            withStartMessage = withStartMessage
        )
        assertStartDownloadNode(chatFile, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onDownloadClicked for chat files launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val chatId = 11L
        val messageId1 = 22L
        val messageId2 = 33L
        val chatFile1 = mock<ChatDefaultFile>()
        val chatFile2 = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId1)).thenReturn(chatFile1)
        whenever(getChatFileUseCase(chatId, messageId2)).thenReturn(chatFile2)
        underTest.onDownloadClicked(
            chatId = chatId,
            messageIds = listOf(messageId1, messageId2),
            withStartMessage = withStartMessage
        )
        assertStartDownloadNode(chatFile1, chatFile2, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onSaveOfflineClicked for chat file launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val chatId = 11L
        val messageId = 22L
        val chatFile = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
        underTest.onSaveOfflineClicked(
            chatId = chatId,
            messageId = messageId,
            withStartMessage = withStartMessage
        )
        assertStartDownloadForOffline(chatFile, withStartMessage = withStartMessage)
    }

    @ParameterizedTest(name = " and withStartMessage set to {0}")
    @ValueSource(booleans = [true, false])
    fun `test that onSaveOfflineClicked with serialized data launches the correct event`(
        withStartMessage: Boolean,
    ) = runTest {
        val node = mock<PublicLinkFile>()
        val serializedData = "serialized data"
        whenever(getPublicNodeFromSerializedDataUseCase(serializedData)).thenReturn(node)
        underTest.onSaveOfflineClicked(
            serializedData = serializedData,
            withStartMessage = withStartMessage
        )
        assertStartDownloadForOffline(node, withStartMessage = withStartMessage)
    }

    @Test
    fun `test that onCopyUriClicked launches the correct event`() = runTest {
        val uri = mock<Uri>()
        underTest.onCopyUriClicked("name", uri)
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.CopyUri::class.java)
            assertThat((content as TransferTriggerEvent.CopyUri).uri).isEqualTo(uri)
        }
    }

    // test for onCopyOfflineNodeClicked
    @Test
    fun `test that onCopyOfflineNodeClicked launches the correct event`() = runTest {
        val nodeId = NodeId(1L)
        underTest.onCopyOfflineNodeClicked(listOf(nodeId))
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.CopyOfflineNode::class.java)
            assertThat((content as TransferTriggerEvent.CopyOfflineNode).nodeIds).isEqualTo(
                listOf(nodeId)
            )
        }
    }

    @Test
    fun `test that onCancelPreviewDownload launches the correct event`() = runTest {
        val tag = 1

        underTest.onCancelPreviewDownload(tag)

        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.CancelPreviewDownload::class.java)
            assertThat((content as TransferTriggerEvent.CancelPreviewDownload).transferTag)
                .isEqualTo(tag)
        }
    }

    private suspend fun assertStartDownloadNode(vararg node: TypedNode, withStartMessage: Boolean) {
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            assertThat((content as TransferTriggerEvent.StartDownloadNode).nodes)
                .containsExactly(*node)
            assertThat(content.withStartMessage).isEqualTo(withStartMessage)
        }
    }

    private suspend fun assertStartDownloadForOffline(node: TypedNode, withStartMessage: Boolean) {
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
            assertThat((content as TransferTriggerEvent.StartDownloadForOffline).node)
                .isEqualTo(node)
            assertThat(content.withStartMessage).isEqualTo(withStartMessage)
        }
    }
}