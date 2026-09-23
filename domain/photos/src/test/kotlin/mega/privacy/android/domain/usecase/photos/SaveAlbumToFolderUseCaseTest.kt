package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.AlbumRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SaveAlbumToFolderUseCaseTest {
    private lateinit var underTest: SaveAlbumToFolderUseCase

    private val albumRepository = mock<AlbumRepository>()

    @Before
    fun setUp() {
        underTest = SaveAlbumToFolderUseCase(
            albumRepository = albumRepository,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        // given
        val folderName = "Folder"
        val photoIds = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )
        val targetParentFolderNodeId = NodeId(1L)

        whenever(albumRepository.saveAlbumToFolder(folderName, photoIds, targetParentFolderNodeId))
            .thenReturn(photoIds)

        // when
        val actualPhotoIds = underTest(folderName, photoIds, targetParentFolderNodeId)

        // then
        assertThat(actualPhotoIds).isEqualTo(photoIds)
    }
}
