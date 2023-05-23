package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetPublicAlbumNodeDataUseCaseTest {
    private val mockGetPublicAlbumNodesDataUseCase: GetPublicAlbumNodesDataUseCase = mock()

    @Test
    fun `test that use case returns correct result`() {
        // given
        val nodesData = mapOf(
            NodeId(1L) to "data1",
            NodeId(2L) to "data2",
            NodeId(3L) to "data3",
        )

        whenever(mockGetPublicAlbumNodesDataUseCase())
            .thenReturn(nodesData)

        // when
        val underTest = GetPublicAlbumNodeDataUseCase(
            getPublicAlbumNodesDataUseCase = mockGetPublicAlbumNodesDataUseCase,
        )
        val actualNodeData = underTest(nodeId = NodeId(1L))

        // then
        assertThat(actualNodeData).isNotNull()
    }
}
