package mega.privacy.android.domain.usecase.node.publiclink

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MapTypedNodeToPublicLinkUseCaseTest {
    private val underTest = MapTypedNodeToPublicLinkUseCase()

    private val typedFileNode = mock<TypedFileNode>()
    private val typedFolderNode = mock<TypedFolderNode>()

    @Test
    internal fun `test that invoke maps a file node to PublicLinkFile`() {
        val actual = underTest(typedFileNode)

        assertThat(actual).isInstanceOf(PublicLinkFile::class.java)
        assertThat((actual as PublicLinkFile).node).isEqualTo(typedFileNode)
    }

    @Test
    internal fun `test that invoke maps a folder node to PublicLinkFolder`() {
        val actual = underTest(typedFolderNode)

        assertThat(actual).isInstanceOf(PublicLinkFolder::class.java)
        assertThat((actual as PublicLinkFolder).node).isEqualTo(typedFolderNode)
    }

    @Test
    internal fun `test that invoke throws when the node is neither a file nor a folder`() {
        assertThrows<IllegalStateException> { underTest(mock<TypedNode>()) }
    }
}
