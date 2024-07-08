package mega.privacy.android.app.presentation.bottomsheet

import android.net.Uri
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.model.TransferTriggerEvent
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.cache.GetCacheFileUseCase
import mega.privacy.android.domain.usecase.filelink.GetPublicNodeFromSerializedDataUseCase
import mega.privacy.android.domain.usecase.folderlink.GetPublicChildNodeFromIdUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.DownloadNodesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

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
    private val getCacheFileUseCase = mock<GetCacheFileUseCase>()
    private val downloadNodesUseCase = mock<DownloadNodesUseCase>()

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
            getCacheFileUseCase,
            downloadNodesUseCase,
        )
    }

    private fun initViewModel() {
        underTest = StartDownloadViewModel(
            getChatFileUseCase,
            getNodeByIdUseCase,
            getPublicNodeFromSerializedDataUseCase,
            getPublicChildNodeFromIdUseCase,
            getCacheFileUseCase,
            downloadNodesUseCase,
        )
    }

    @Test
    fun `test that onDownloadClicked launches the correct event`() = runTest {
        val node = mock<TypedFileNode>()
        underTest.onDownloadClicked(node)
        assertStartDownloadNode(node)
    }

    @Test
    fun `test that onSaveOfflineClicked launches the correct event`() = runTest {
        val node = mock<TypedFileNode>()
        underTest.onSaveOfflineClicked(node)
        assertStartDownloadForOffline(node)
    }

    @Test
    fun `test that onDownloadClicked with id launches the correct event`() = runTest {
        val nodeId = NodeId(1L)
        val node = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        underTest.onDownloadClicked(nodeId)
        assertStartDownloadNode(node)
    }

    @Test
    fun `test that onDownloadClicked with serialized data launches the correct event`() = runTest {
        val node = mock<PublicLinkFile>()
        val serializedData = "serialized data"
        whenever(getPublicNodeFromSerializedDataUseCase(serializedData)).thenReturn(node)
        underTest.onDownloadClicked(serializedData)
        assertStartDownloadNode(node)
    }

    @Test
    fun `test that onFolderLinkChildNodeDownloadClicked  launches the correct event`() = runTest {
        val node = mock<PublicLinkFile>()
        val id = NodeId(1L)
        whenever(getPublicChildNodeFromIdUseCase(id)).thenReturn(node)
        underTest.onFolderLinkChildNodeDownloadClicked(id)
        assertStartDownloadNode(node)
    }

    @Test
    fun `test that onSaveOfflineClicked with id launches the correct event`() = runTest {
        val nodeId = NodeId(1L)
        val node = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        underTest.onSaveOfflineClicked(nodeId)
        assertStartDownloadForOffline(node)
    }

    @Test
    fun `test that onDownloadClicked for chat file launches the correct event`() = runTest {
        val chatId = 11L
        val messageId = 22L
        val chatFile = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
        underTest.onDownloadClicked(chatId, messageId)
        assertStartDownloadNode(chatFile)
    }

    @Test
    fun `test that onDownloadClicked for chat files launches the correct event`() = runTest {
        val chatId = 11L
        val messageId1 = 22L
        val messageId2 = 33L
        val chatFile1 = mock<ChatDefaultFile>()
        val chatFile2 = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId1)).thenReturn(chatFile1)
        whenever(getChatFileUseCase(chatId, messageId2)).thenReturn(chatFile2)
        underTest.onDownloadClicked(chatId, listOf(messageId1, messageId2))
        assertStartDownloadNode(chatFile1, chatFile2)
    }

    @Test
    fun `test that onSaveOfflineClicked for chat file launches the correct event`() = runTest {
        val chatId = 11L
        val messageId = 22L
        val chatFile = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
        underTest.onSaveOfflineClicked(chatId, messageId)
        assertStartDownloadForOffline(chatFile)
    }

    @Test
    fun `test that onSaveOfflineClicked with serialized data launches the correct event`() =
        runTest {
            val node = mock<PublicLinkFile>()
            val serializedData = "serialized data"
            whenever(getPublicNodeFromSerializedDataUseCase(serializedData)).thenReturn(node)
            underTest.onSaveOfflineClicked(serializedData)
            assertStartDownloadForOffline(node)
        }

    @Test
    fun `test that downloadVoiceClip downloads the node to the cache folder`() = runTest {
        val chatId = 34L
        val messageId = 65L
        val chatFile = mock<ChatDefaultFile>()
        val cachePath = "cache/"
        val cacheFile = File("$cachePath/file.mp3")
        val downloadFlow = mock<Flow<MultiTransferEvent>>()
        whenever(getChatFileUseCase(chatId, messageId)) doReturn chatFile
        whenever(getCacheFileUseCase(any(), any())) doReturn cacheFile
        whenever(
            downloadNodesUseCase(
                nodes = listOf(chatFile),
                destinationPath = cachePath,
                appData = TransferAppData.VoiceClip,
                isHighPriority = true,
            )
        ) doReturn downloadFlow

        underTest.downloadVoiceClip("messageName", chatId, messageId)

        verify(downloadFlow).collect(any())
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

    private suspend fun assertStartDownloadNode(vararg node: TypedNode) {
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            assertThat((content as TransferTriggerEvent.StartDownloadNode).nodes)
                .containsExactly(*node)
        }
    }

    private suspend fun assertStartDownloadForOffline(node: TypedNode) {
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
            assertThat((content as TransferTriggerEvent.StartDownloadForOffline).node)
                .isEqualTo(node)
        }
    }
}