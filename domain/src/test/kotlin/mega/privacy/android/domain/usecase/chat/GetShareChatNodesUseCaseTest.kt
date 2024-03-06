package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeShareContentUri
import mega.privacy.android.domain.entity.node.chat.ChatImageFile
import mega.privacy.android.domain.usecase.node.GetNodePreviewFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetShareChatNodesUseCaseTest {
    private val exportChatNodesUseCase: ExportChatNodesUseCase = mock()
    private val getNodePreviewFileUseCase: GetNodePreviewFileUseCase = mock()
    private lateinit var underTest: GetShareChatNodesUseCase

    @BeforeAll
    fun setup() {
        underTest = GetShareChatNodesUseCase(
            exportChatNodesUseCase,
            getNodePreviewFileUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(exportChatNodesUseCase, getNodePreviewFileUseCase)
    }

    @Test
    fun `test that local uris returns when all nodes downloaded`() = runTest {
        whenever(getNodePreviewFileUseCase.invoke(any())).thenReturn(File("path"))
        val chatNodes = listOf<ChatImageFile>(mock(), mock())
        val actual = underTest.invoke(chatNodes)
        Truth.assertThat(actual).isInstanceOf(NodeShareContentUri.LocalContentUris::class.java)
    }

    @Test
    fun `test that remote uris returns when some nodes not downloaded`() = runTest {
        val node1 = mock<ChatImageFile>()
        val node2 = mock<ChatImageFile>()
        whenever(getNodePreviewFileUseCase.invoke(node1)).thenReturn(File("path"))
        whenever(getNodePreviewFileUseCase.invoke(node2)).thenReturn(null)
        val chatNodes = listOf(node1, node2)
        whenever(exportChatNodesUseCase.invoke(chatNodes)).thenReturn(mapOf(NodeId(1L) to "link1"))
        val actual = underTest.invoke(chatNodes)
        Truth.assertThat(actual).isInstanceOf(NodeShareContentUri.RemoteContentUris::class.java)
    }
}