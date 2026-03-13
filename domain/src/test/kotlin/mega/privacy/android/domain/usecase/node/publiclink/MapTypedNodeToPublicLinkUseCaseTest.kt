package mega.privacy.android.domain.usecase.node.publiclink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFile
import mega.privacy.android.domain.entity.node.publiclink.PublicLinkFolder
import mega.privacy.android.domain.usecase.AddNodeType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import kotlin.test.Ignore

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MapTypedNodeToPublicLinkUseCaseTest {
    private lateinit var underTest: MapTypedNodeToPublicLinkUseCase

    private val typedFolderNode = mock<TypedFolderNode>()
    private val typedFileNode = mock<TypedFileNode>()

    private val addNodeType = mock<AddNodeType> {
        onBlocking { invoke(argWhere { it is FileNode }) }.thenReturn(typedFileNode)
        onBlocking { invoke(argWhere { it is FolderNode }) }.thenReturn(typedFolderNode)
    }

    private val monitorPublicLinkFolderUseCase = mock<MonitorPublicLinkFolderUseCase>()

    @BeforeAll
    internal fun setUp() {
        underTest = MapTypedNodeToPublicLinkUseCase(
            addNodeType = addNodeType,
            monitorPublicLinkFolderUseCase = monitorPublicLinkFolderUseCase,
        )
    }

    @Test
    internal fun `test that file without parent is mapped to PublicLinkFile`() = runTest {
        val actual = underTest(typedFileNode)

        assertThat(actual).isInstanceOf(PublicLinkFile::class.java)
        assertThat(actual.parent).isNull()
    }

    @Test
    internal fun `test that file with parent is mapped to PublicLinkFile with correct parent`() =
        runTest {
            val parent = mock<PublicLinkFolder>()

            val actual = underTest(typedFileNode, parent)

            assertThat(actual).isInstanceOf(PublicLinkFile::class.java)
            assertThat(actual.parent).isEqualTo(parent)
        }

    @Test
    internal fun `test that folder without parent is mapped to PublicLinkFolder`() = runTest {
        val actual = underTest(typedFolderNode)

        assertThat(actual).isInstanceOf(PublicLinkFolder::class.java)
        assertThat(actual.parent).isNull()
    }

    @Test
    internal fun `test that folder with parent is mapped to PublicLinkFolder with correct parent`() =
        runTest {
            val parent = mock<PublicLinkFolder>()

            val actual = underTest(typedFolderNode, parent)

            assertThat(actual).isInstanceOf(PublicLinkFolder::class.java)
            assertThat(actual.parent).isEqualTo(parent)
        }

    // suspend high order function cannot be mocked on Kotlin 2.0
    @Ignore
    @Test
    internal fun `test that fetching children returns mapped public link nodes`() = runTest {
        val children = listOf(mock<FileNode>(), mock<FolderNode>())

        monitorPublicLinkFolderUseCase.stub {
            on { invoke(any()) }.thenReturn(flowOf(children))
        }

        val folder = underTest(typedFolderNode) as PublicLinkFolder
        folder.children.test {
            val actual = awaitItem()
            assertThat(actual).hasSize(children.size)
            assertThat(actual[0]).isInstanceOf(PublicLinkFile::class.java)
            assertThat(actual[1]).isInstanceOf(PublicLinkFolder::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
