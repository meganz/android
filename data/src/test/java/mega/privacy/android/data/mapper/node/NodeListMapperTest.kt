package mega.privacy.android.data.mapper.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeListMapperTest {

    private lateinit var underTest: NodeListMapper
    private lateinit var nodeMapper: NodeMapper

    @BeforeAll
    fun setup() {
        nodeMapper = mock()
        underTest = NodeListMapper(nodeMapper)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test that node list mapper returns correctly`() = runTest {
        val node1 = mock<MegaNode>()
        val node2 = mock<MegaNode>()
        val node3 = mock<MegaNode>()
        val mappedNode1 = mock<FileNode>()
        val mappedNode2 = mock<FileNode>()
        val mappedNode3 = mock<FileNode>()
        val megaNodeList = mock<MegaNodeList> {
            on { get(0) }.thenReturn(node1)
            on { get(1) }.thenReturn(node2)
            on { get(2) }.thenReturn(node3)
            on { size() }.thenReturn(3)
        }
        whenever(nodeMapper(node1)).thenReturn(mappedNode1)
        whenever(nodeMapper(node2)).thenReturn(mappedNode2)
        whenever(nodeMapper(node3)).thenReturn(mappedNode3)
        val nodeList = listOf(mappedNode1, mappedNode2, mappedNode3)
        Truth.assertThat(underTest.invoke(megaNodeList)).isEqualTo(nodeList)
    }
}