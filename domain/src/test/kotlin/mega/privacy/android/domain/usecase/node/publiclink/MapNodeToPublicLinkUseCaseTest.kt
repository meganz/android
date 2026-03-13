package mega.privacy.android.domain.usecase.node.publiclink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkNode
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MapNodeToPublicLinkUseCaseTest {
    private lateinit var underTest: MapNodeToPublicLinkUseCase

    private val typedFileNode = mock<TypedFileNode>()
    private val typedFolderNode = mock<TypedFolderNode>()

    private val addNodeType = mock<AddNodeType> {
        onBlocking { invoke(argWhere { it is FileNode }) }.thenReturn(typedFileNode)
        onBlocking { invoke(argWhere { it is FolderNode }) }.thenReturn(typedFolderNode)
    }

    private val publicLinkFile = mock<PublicLinkFile>()
    private val publicLinkFolder = mock<PublicLinkFolder>()

    private val mapTypedNodeToPublicLinkUseCase = mock<MapTypedNodeToPublicLinkUseCase> {
        on { invoke(eq(typedFileNode), any()) }.thenReturn(publicLinkFile)
        on { invoke(eq(typedFolderNode), any()) }.thenReturn(publicLinkFolder)
    }

    @BeforeAll
    internal fun setUp() {
        underTest = MapNodeToPublicLinkUseCase(
            addNodeType = addNodeType,
            mapTypedNodeToPublicLinkUseCase = mapTypedNodeToPublicLinkUseCase,
        )
    }

    @Test
    internal fun `test that file node is delegated to mapTypedNodeToPublicLinkUseCase`() = runTest {
        val parent = mock<PublicLinkFolder>()

        val actual = underTest(mock<FileNode>(), parent)

        verify(mapTypedNodeToPublicLinkUseCase).invoke(typedFileNode, parent)
        assertThat(actual).isEqualTo(publicLinkFile)
    }

    @Test
    internal fun `test that folder node is delegated to mapTypedNodeToPublicLinkUseCase`() =
        runTest {
            val parent = mock<PublicLinkFolder>()

            val actual = underTest(mock<FolderNode>(), parent)

            verify(mapTypedNodeToPublicLinkUseCase).invoke(typedFolderNode, parent)
            assertThat(actual).isEqualTo(publicLinkFolder)
        }

    @Test
    internal fun `test that null parent is passed through to mapTypedNodeToPublicLinkUseCase`() =
        runTest {
            underTest(mock<FileNode>(), null)

            verify(mapTypedNodeToPublicLinkUseCase).invoke(typedFileNode, null)
        }
}
