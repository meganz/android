package mega.privacy.android.domain.usecase.node.publiclink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
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

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MapNodeToPublicLinkUseCaseTest {
    private lateinit var underTest: MapNodeToPublicLinkUseCase

    private val typedFolderNode = mock<TypedFolderNode>()

    private val addNodeType = mock<AddNodeType> {
        onBlocking { invoke(argWhere { it is FileNode }) }.thenReturn(mock<TypedFileNode>())
        onBlocking { invoke(argWhere { it is FolderNode }) }.thenReturn(typedFolderNode)
    }

    private val monitorPublicLinkFolderUseCase = mock<MonitorPublicLinkFolderUseCase>()

    @BeforeAll
    internal fun setUp() {
        underTest = MapNodeToPublicLinkUseCase(
            addNodeType = addNodeType,
            monitorPublicLinkFolderUseCase = monitorPublicLinkFolderUseCase,
            getCloudSortOrder = mock { onBlocking { invoke() }.thenReturn(SortOrder.ORDER_DEFAULT_ASC) },
        )
    }

    @Test
    internal fun `test that file without parent is mapped correctly`() = runTest {
        val actual = underTest(mock<FileNode>(), null)

        assertThat(actual).isInstanceOf(PublicLinkFile::class.java)
        assertThat(actual.parent).isNull()
    }

    @Test
    internal fun `test that file with parent is mapped correctly`() = runTest {
        val parent = mock<PublicLinkFolder>()
        val actual = underTest(mock<FileNode>(), parent)

        assertThat(actual).isInstanceOf(PublicLinkFile::class.java)
        assertThat(actual.parent).isEqualTo(parent)
    }

    @Test
    internal fun `test that a folder node without parent is mapped correctly`() = runTest {
        val actual = underTest(mock<FolderNode>(), null)

        assertThat(actual).isInstanceOf(PublicLinkFolder::class.java)
        assertThat(actual.parent).isNull()
    }

    @Test
    internal fun `test that folder with parent is mapped correctly`() = runTest {
        val parent = mock<PublicLinkFolder>()
        val actual = underTest(mock<FolderNode>(), parent)

        assertThat(actual).isInstanceOf(PublicLinkFolder::class.java)
        assertThat(actual.parent).isEqualTo(parent)
    }

    @Test
    internal fun `test that fetching children return mapped public links`() = runTest {
        val children = listOf(mock<FileNode>(), mock<FolderNode>())

        monitorPublicLinkFolderUseCase.stub {
            on { invoke(any()) }.thenReturn(flowOf(children))
        }

        val parent = mock<FolderNode> {
            on { fetchChildren }.thenReturn { listOf(mock()) }
        }
        val folder = underTest(parent, null) as PublicLinkFolder
        folder.children.test {
            val actual = awaitItem()
            assertThat(actual).hasSize(children.size)
            assertThat(actual[0]).isInstanceOf(PublicLinkFile::class.java)
            assertThat(actual[1]).isInstanceOf(PublicLinkFolder::class.java)
            cancelAndIgnoreRemainingEvents()
        }

    }
}