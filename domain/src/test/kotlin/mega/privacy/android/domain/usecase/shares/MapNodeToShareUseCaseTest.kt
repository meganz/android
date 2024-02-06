package mega.privacy.android.domain.usecase.shares


import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.shares.ShareFileNode
import mega.privacy.android.domain.entity.node.shares.ShareFolderNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MapNodeToShareUseCaseTest {
    private lateinit var underTest: MapNodeToShareUseCase

    @BeforeAll
    internal fun setUp() {
        underTest = MapNodeToShareUseCase()
    }

    @Test
    internal fun `test that file node without share data is mapped correctly`() = runTest {
        val actual = underTest(mock<TypedFileNode>(), null)

        assertThat(actual).isInstanceOf(ShareFileNode::class.java)
        assertThat(actual.shareData).isNull()
    }

    @Test
    internal fun `test that file node with share data is mapped correctly`() = runTest {
        val shareData = mock<ShareData>()
        val actual = underTest(mock<TypedFileNode>(), shareData)

        assertThat(actual).isInstanceOf(ShareFileNode::class.java)
        assertThat(actual.shareData).isEqualTo(shareData)
    }

    @Test
    internal fun `test that a folder node without share data is mapped correctly`() = runTest {
        val actual = underTest(mock<TypedFolderNode>(), null)

        assertThat(actual).isInstanceOf(ShareFolderNode::class.java)
        assertThat(actual.shareData).isNull()
    }

    @Test
    internal fun `test that folder with share data is mapped correctly`() = runTest {
        val shareData = mock<ShareData>()
        val actual = underTest(mock<TypedFolderNode>(), shareData)

        assertThat(actual).isInstanceOf(ShareFolderNode::class.java)
        assertThat(actual.shareData).isEqualTo(shareData)
    }
}