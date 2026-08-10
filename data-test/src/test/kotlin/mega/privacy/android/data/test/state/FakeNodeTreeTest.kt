package mega.privacy.android.data.test.state

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.test.stub.StubMegaNode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeNodeTreeTest {

    private lateinit var underTest: FakeNodeTree

    @BeforeEach
    fun setUp() {
        underTest = FakeNodeTree()
    }

    @Test
    fun `test that the tree is seeded with the three root nodes when created`() {
        assertThat(underTest.rootNode.handle).isEqualTo(1L)
        assertThat(underTest.rootNode.name).isEqualTo("Cloud Drive")
        assertThat(underTest.rootNode.isFolder).isTrue()
        assertThat(underTest.rubbishBinNode.handle).isEqualTo(2L)
        assertThat(underTest.rubbishBinNode.name).isEqualTo("Rubbish Bin")
        assertThat(underTest.vaultNode.handle).isEqualTo(3L)
        assertThat(underTest.vaultNode.name).isEqualTo("Vault")
    }

    @Test
    fun `test that nodeByHandle resolves the seeded roots when queried`() {
        assertThat(underTest.nodeByHandle(1L)).isSameInstanceAs(underTest.rootNode)
        assertThat(underTest.nodeByHandle(2L)).isSameInstanceAs(underTest.rubbishBinNode)
        assertThat(underTest.nodeByHandle(3L)).isSameInstanceAs(underTest.vaultNode)
    }

    @Test
    fun `test that nodeByHandle returns null when the handle is unknown`() {
        assertThat(underTest.nodeByHandle(999L)).isNull()
    }

    @Test
    fun `test that nodeByHandle resolves an added node when it was added`() {
        val node = StubMegaNode(handle = 10L, name = "photo.jpg")

        underTest.addNode(node, parentHandle = 1L)

        assertThat(underTest.nodeByHandle(10L)).isSameInstanceAs(node)
    }

    @Test
    fun `test that childrenOf returns the added children in insertion order`() {
        val first = StubMegaNode(handle = 10L, name = "a.jpg")
        val second = StubMegaNode(handle = 11L, name = "b.jpg")
        val elsewhere = StubMegaNode(handle = 12L, name = "c.jpg")
        underTest.addNode(first, parentHandle = 1L)
        underTest.addNode(second, parentHandle = 1L)
        underTest.addNode(elsewhere, parentHandle = 2L)

        assertThat(underTest.childrenOf(1L)).containsExactly(first, second).inOrder()
        assertThat(underTest.childrenOf(2L)).containsExactly(elsewhere)
    }

    @Test
    fun `test that childrenOf returns empty when the parent has no children`() {
        assertThat(underTest.childrenOf(1L)).isEmpty()
    }

    @Test
    fun `test that addNode replaces the existing node when the handle already exists`() {
        underTest.addNode(StubMegaNode(handle = 10L, name = "old.jpg"), parentHandle = 1L)
        val replacement = StubMegaNode(handle = 10L, name = "new.jpg")

        underTest.addNode(replacement, parentHandle = 1L)

        assertThat(underTest.nodeByHandle(10L)).isSameInstanceAs(replacement)
        assertThat(underTest.childrenOf(1L)).containsExactly(replacement)
    }

    @Test
    fun `test that removeNode removes the node when it exists`() {
        underTest.addNode(StubMegaNode(handle = 10L), parentHandle = 1L)

        underTest.removeNode(10L)

        assertThat(underTest.nodeByHandle(10L)).isNull()
        assertThat(underTest.childrenOf(1L)).isEmpty()
    }

    @Test
    fun `test that removeNode removes descendants when the node has children`() {
        val folder = StubMegaNode(handle = 10L, isFolder = true)
        val child = StubMegaNode(handle = 11L)
        underTest.addNode(folder, parentHandle = 1L)
        underTest.addNode(child, parentHandle = 10L)

        underTest.removeNode(10L)

        assertThat(underTest.nodeByHandle(10L)).isNull()
        assertThat(underTest.nodeByHandle(11L)).isNull()
    }

    @Test
    fun `test that clear restores the seeded roots when nodes were added`() {
        underTest.addNode(StubMegaNode(handle = 10L), parentHandle = 1L)

        underTest.clear()

        assertThat(underTest.nodeByHandle(10L)).isNull()
        assertThat(underTest.nodeByHandle(1L)).isSameInstanceAs(underTest.rootNode)
        assertThat(underTest.nodeByHandle(2L)).isSameInstanceAs(underTest.rubbishBinNode)
        assertThat(underTest.nodeByHandle(3L)).isSameInstanceAs(underTest.vaultNode)
    }
}
