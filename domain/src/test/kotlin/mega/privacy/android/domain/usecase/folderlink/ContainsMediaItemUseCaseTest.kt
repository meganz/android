package mega.privacy.android.domain.usecase.folderlink

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ContainsMediaItemUseCaseTest {
    private lateinit var underTest: ContainsMediaItemUseCase

    @Before
    fun setup() {
        underTest = ContainsMediaItemUseCase()
    }

    @Test
    fun `test that list containing only folders returns false`() = runTest {
        val node1 = mock<TypedFolderNode>()
        val node2 = mock<TypedFolderNode>()
        val nodeList: List<TypedNode> = listOf(node1, node2)

        assertThat(underTest(nodeList)).isEqualTo(false)
    }

    @Test
    fun `test that list containing only image returns true`() = runTest {
        val node1 = mock<TypedFileNode>()
        val nodeList: List<TypedNode> = listOf(node1)

        whenever(node1.type).thenReturn(StaticImageFileTypeInfo("image", "jpg"))
        assertThat(underTest(nodeList)).isEqualTo(true)
    }

    @Test
    fun `test that list containing only video returns true`() = runTest {
        val node1 = mock<TypedFileNode>()
        val nodeList: List<TypedNode> = listOf(node1)

        whenever(node1.type).thenReturn(VideoFileTypeInfo("image", "mp4", 10))
        assertThat(underTest(nodeList)).isEqualTo(true)
    }

    @Test
    fun `test that list containing only svg file returns true`() = runTest {
        val node1 = mock<TypedFileNode>()
        val nodeList: List<TypedNode> = listOf(node1)

        whenever(node1.type).thenReturn(SvgFileTypeInfo("image", "svg"))
        assertThat(underTest(nodeList)).isEqualTo(false)
    }

    @Test
    fun `test that list containing folder, svg, image, video returns true`() = runTest {
        val node1 = mock<TypedFileNode>()
        val node2 = mock<TypedFileNode>()
        val node3 = mock<TypedFileNode>()
        val node4 = mock<TypedFolderNode>()
        val nodeList: List<TypedNode> = listOf(node1, node2, node3, node4)

        whenever(node1.type).thenReturn(VideoFileTypeInfo("video", "mp4", 10))
        whenever(node2.type).thenReturn(StaticImageFileTypeInfo("image", "jpg"))
        whenever(node3.type).thenReturn(SvgFileTypeInfo("image", "svg"))
        assertThat(underTest(nodeList)).isEqualTo(true)
    }
}