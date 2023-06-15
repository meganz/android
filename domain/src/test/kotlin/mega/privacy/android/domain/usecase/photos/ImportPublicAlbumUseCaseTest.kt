package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.photos.AlbumId
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.fail

@ExperimentalCoroutinesApi
class ImportPublicAlbumUseCaseTest {
    private lateinit var underTest: ImportPublicAlbumUseCase

    private val saveAlbumToFolderUseCase: SaveAlbumToFolderUseCase = mock()

    private val createAlbumUseCase: CreateAlbumUseCase = mock()

    private val addPhotosToAlbumUseCase: AddPhotosToAlbumUseCase = mock()

    @Before
    fun setUp() {
        underTest = ImportPublicAlbumUseCase(
            saveAlbumToFolderUseCase,
            createAlbumUseCase,
            addPhotosToAlbumUseCase,
        )
    }

    @Test
    fun `test that use case returns correct result`() = runTest {
        val albumName = "Album"
        val albumId = AlbumId(1L)

        val album = mock<Album.UserAlbum>()
        val photoIds = listOf(
            NodeId(1L),
            NodeId(2L),
            NodeId(3L),
        )
        val targetParentFolderNodeId = NodeId(1L)

        whenever(createAlbumUseCase(albumName))
            .thenReturn(album)

        whenever(saveAlbumToFolderUseCase(albumName, photoIds, targetParentFolderNodeId))
            .thenReturn(photoIds)

        whenever(addPhotosToAlbumUseCase(albumId, photoIds))
            .thenReturn(photoIds.size)

        try {
            underTest(albumName, photoIds, targetParentFolderNodeId)
        } catch (e: Exception) {
            fail(message = "${e.message}")
        }
    }
}
