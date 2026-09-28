package mega.privacy.android.app.presentation.folderlink.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class FolderLinkStateTest {

    @Test
    fun `test that showContentActions is true when nodes are fetched without errors`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError
        )
        assertThat(folderLinkState.showContentActions).isTrue()
    }

    @Test
    fun `test that showDecryptedActions is true when the parent node is decrypted`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError,
            parentNode = folderNode(isNodeKeyDecrypted = true)
        )
        assertThat(folderLinkState.showDecryptedActions).isTrue()
    }

    @Test
    fun `test that showDecryptedActions is false when the parent node is not decrypted`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError,
            parentNode = folderNode(isNodeKeyDecrypted = false)
        )
        assertThat(folderLinkState.showDecryptedActions).isFalse()
    }

    @Test
    fun `test that showDecryptedActions is false when there is no parent node`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError,
            parentNode = null
        )
        assertThat(folderLinkState.showDecryptedActions).isFalse()
    }

    @Test
    fun `test that showDecryptedActions is false when the nodes are not fetched`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = false,
            errorState = LinkErrorState.NoError,
            parentNode = folderNode(isNodeKeyDecrypted = true)
        )
        assertThat(folderLinkState.showDecryptedActions).isFalse()
    }

    @Test
    fun `test that showShareAction is true at the root of the link`() {
        val root = folderNode(handle = 1L, isNodeKeyDecrypted = true)
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError,
            rootNode = root,
            parentNode = root
        )
        assertThat(folderLinkState.showShareAction).isTrue()
    }

    @Test
    fun `test that showShareAction is false inside a subfolder`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError,
            rootNode = folderNode(handle = 1L, isNodeKeyDecrypted = true),
            parentNode = folderNode(handle = 2L, isNodeKeyDecrypted = true)
        )
        assertThat(folderLinkState.showShareAction).isFalse()
        assertThat(folderLinkState.showDecryptedActions).isTrue()
    }

    @Test
    fun `test that showShareAction is false when the root is not decrypted`() {
        val root = folderNode(handle = 1L, isNodeKeyDecrypted = false)
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError,
            rootNode = root,
            parentNode = root
        )
        assertThat(folderLinkState.showShareAction).isFalse()
    }

    @Test
    fun `test that showShareAction is false when there is no root node`() {
        val folderLinkState = FolderLinkState(
            isNodesFetched = true,
            errorState = LinkErrorState.NoError,
            rootNode = null,
            parentNode = folderNode(handle = 2L, isNodeKeyDecrypted = true)
        )
        assertThat(folderLinkState.showShareAction).isFalse()
    }

    private fun folderNode(
        isNodeKeyDecrypted: Boolean,
        handle: Long = 1L,
    ) = mock<TypedFolderNode> {
        on { this.isNodeKeyDecrypted }.thenReturn(isNodeKeyDecrypted)
        on { id }.thenReturn(NodeId(handle))
    }
}
