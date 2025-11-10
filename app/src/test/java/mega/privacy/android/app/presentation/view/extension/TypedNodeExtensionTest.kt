package mega.privacy.android.app.presentation.view.extension

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TypedNodeExtensionTest {

    @Test
    fun `test that isNotS4Container returns true for file node`() {
        val fileNode = mock<TypedFileNode>()

        val result = fileNode.isNotS4Container()

        assertThat(result).isTrue()
    }

    @Test
    fun `test that isNotS4Container returns true for folder node that is not S4 container`() {
        val folderNode = mock<TypedFolderNode> {
            on { isS4Container }.doReturn(false)
        }

        val result = folderNode.isNotS4Container()

        assertThat(result).isTrue()
    }

    @Test
    fun `test that isNotS4Container returns false for folder node that is S4 container`() {
        val folderNode = mock<TypedFolderNode> {
            on { isS4Container }.doReturn(true)
        }

        val result = folderNode.isNotS4Container()

        assertThat(result).isFalse()
    }

    @Test
    fun `test that isNotS4Container returns true for non-folder typed node`() {
        val typedNode = mock<TypedNode>()

        val result = typedNode.isNotS4Container()

        assertThat(result).isTrue()
    }
}

