package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class GetPublicAlbumNodesDataUseCaseTest {
    private val mockAlbumRepository: AlbumRepository = mock()

    @Test
    fun `test that use case returns correct result`() {
        // given
        val nodesData = mapOf(
            NodeId(1L) to "data1",
            NodeId(2L) to "data2",
            NodeId(3L) to "data3",
        )

        whenever(mockAlbumRepository.getPublicAlbumNodesData())
            .thenReturn(nodesData)

        // when
        val underTest = GetPublicAlbumNodesDataUseCase(
            albumRepository = mockAlbumRepository,
        )
        val actualNodesData = underTest()

        // then
        assertThat(actualNodesData).isEqualTo(nodesData)
    }
}
