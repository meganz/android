package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import mega.privacy.android.domain.usecase.node.GetAncestorsIdsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPhotosByFolderIdUseCaseTest {

    private lateinit var underTest: GetPhotosByFolderIdUseCase

    private val photosRepository = mock<PhotosRepository>()
    private val nodeRepository = mock<NodeRepository>()
    private val getAncestorsIdsUseCase = mock<GetAncestorsIdsUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetPhotosByFolderIdUseCase(
            photosRepository = photosRepository,
            nodeRepository = nodeRepository,
            getAncestorsIdsUseCase = getAncestorsIdsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(photosRepository, nodeRepository, getAncestorsIdsUseCase)
    }

    @Test
    fun `test that invoke emits the folder photos when there are no node updates`() = runTest {
        val photo = photo(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE)
        stubFolderPhotos(photo)
        stubNodeUpdates()

        assertThat(underTest(folderId, recursive = false).toList())
            .containsExactly(listOf(photo))
    }

    @Test
    fun `test that invoke re-queries when a photo is added to the folder`() = runTest {
        stubFolderPhotos()
        stubNodeUpdates(
            update(mediaNode(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE), NodeChanges.New)
        )

        assertThat(underTest(folderId, recursive = false).toList()).hasSize(2)
    }

    @Test
    fun `test that invoke re-queries for every list affecting change on a photo in the folder`() =
        runTest {
            val listAffectingChanges = listOf(
                NodeChanges.New,
                NodeChanges.Remove,
                NodeChanges.Parent,
                NodeChanges.Timestamp,
                NodeChanges.Sensitive,
                NodeChanges.Favourite,
                NodeChanges.Name,
            )

            listAffectingChanges.forEach { change ->
                stubFolderPhotos(photo(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE))
                stubNodeUpdates(
                    update(mediaNode(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE), change)
                )

                assertThat(underTest(folderId, recursive = false).toList()).hasSize(2)
            }
        }

    @Test
    fun `test that invoke does not re-query when an unrelated media node changes`() = runTest {
        stubFolderPhotos(photo(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE))
        stubNodeUpdates(
            update(mediaNode(id = OTHER_PHOTO_HANDLE, parentId = OTHER_FOLDER_HANDLE), NodeChanges.New)
        )

        assertThat(underTest(folderId, recursive = false).toList()).hasSize(1)
    }

    @Test
    fun `test that invoke does not re-query when a non media node in the folder changes`() =
        runTest {
            val pdfNode = mock<FileNode> {
                on { id } doReturn NodeId(OTHER_PHOTO_HANDLE)
                on { parentId } doReturn NodeId(FOLDER_HANDLE)
                on { type } doReturn PdfFileTypeInfo
            }
            stubFolderPhotos(photo(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE))
            stubNodeUpdates(NodeUpdate(mapOf(pdfNode to listOf(NodeChanges.New))))

            assertThat(underTest(folderId, recursive = false).toList()).hasSize(1)
        }

    @Test
    fun `test that invoke does not re-query for a non list affecting change on a photo in the folder`() =
        runTest {
            stubFolderPhotos(photo(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE))
            stubNodeUpdates(
                update(
                    mediaNode(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE),
                    NodeChanges.Description
                ),
                update(mediaNode(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE), NodeChanges.Tags),
            )

            assertThat(underTest(folderId, recursive = false).toList()).hasSize(1)
        }

    @Test
    fun `test that invoke re-queries when a photo of the folder is removed`() = runTest {
        // A removed photo reports the rubbish bin as its parent, so only the previous query
        // identifies it as one of the folder's own photos.
        stubFolderPhotos(photo(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE))
        stubNodeUpdates(
            update(mediaNode(id = PHOTO_HANDLE, parentId = RUBBISH_HANDLE), NodeChanges.Remove)
        )

        assertThat(underTest(folderId, recursive = false).toList()).hasSize(2)
    }

    @Test
    fun `test that invoke re-queries when a photo is moved out of the folder`() = runTest {
        stubFolderPhotos(photo(id = PHOTO_HANDLE, parentId = FOLDER_HANDLE))
        stubNodeUpdates(
            update(
                mediaNode(id = PHOTO_HANDLE, parentId = OTHER_FOLDER_HANDLE),
                NodeChanges.Parent
            )
        )

        assertThat(underTest(folderId, recursive = false).toList()).hasSize(2)
    }

    @Test
    fun `test that invoke re-queries when a photo is added to a sub folder and recursive`() =
        runTest {
            val subFolderPhoto = mediaNode(id = PHOTO_HANDLE, parentId = SUB_FOLDER_HANDLE)
            stubFolderPhotos()
            stubNodeUpdates(update(subFolderPhoto, NodeChanges.New))
            whenever(getAncestorsIdsUseCase(subFolderPhoto))
                .thenReturn(listOf(NodeId(SUB_FOLDER_HANDLE), folderId))

            assertThat(underTest(folderId, recursive = true).toList()).hasSize(2)
        }

    @Test
    fun `test that invoke does not re-query when a photo is added to a sub folder and not recursive`() =
        runTest {
            stubFolderPhotos()
            stubNodeUpdates(
                update(mediaNode(id = PHOTO_HANDLE, parentId = SUB_FOLDER_HANDLE), NodeChanges.New)
            )

            assertThat(underTest(folderId, recursive = false).toList()).hasSize(1)
        }

    @Test
    fun `test that invoke does not re-query when a photo is added outside the folder tree and recursive`() =
        runTest {
            val outsidePhoto = mediaNode(id = PHOTO_HANDLE, parentId = OTHER_FOLDER_HANDLE)
            stubFolderPhotos()
            stubNodeUpdates(update(outsidePhoto, NodeChanges.New))
            whenever(getAncestorsIdsUseCase(outsidePhoto))
                .thenReturn(listOf(NodeId(OTHER_FOLDER_HANDLE)))

            assertThat(underTest(folderId, recursive = true).toList()).hasSize(1)
        }

    @Test
    fun `test that invoke re-queries for a media node outside the folder when from a folder link`() =
        runTest {
            stubFolderPhotos()
            stubNodeUpdates(
                update(
                    mediaNode(id = PHOTO_HANDLE, parentId = OTHER_FOLDER_HANDLE),
                    NodeChanges.New
                )
            )

            val actual = underTest(folderId, recursive = false, isFromFolderLink = true).toList()

            assertThat(actual).hasSize(2)
        }

    private suspend fun stubFolderPhotos(vararg photos: Photo) {
        whenever(photosRepository.getPhotosByFolderId(any(), any(), any(), any()))
            .thenReturn(photos.toList())
    }

    private fun stubNodeUpdates(vararg updates: NodeUpdate) {
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(*updates))
    }

    private fun update(node: FileNode, vararg changes: NodeChanges) =
        NodeUpdate(mapOf(node to changes.toList()))

    private fun mediaNode(id: Long, parentId: Long) = mock<FileNode> {
        on { this.id } doReturn NodeId(id)
        on { this.parentId } doReturn NodeId(parentId)
        on { type } doReturn StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
    }

    private fun photo(id: Long, parentId: Long) = Photo.Image(
        id = id,
        parentId = parentId,
        name = "",
        isFavourite = false,
        creationTime = LocalDateTime.now(),
        modificationTime = LocalDateTime.now(),
        thumbnailFilePath = "thumbnailFilePath",
        previewFilePath = "previewFilePath",
        fileTypeInfo = StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg"),
    )

    private companion object {
        const val FOLDER_HANDLE = 1L
        const val SUB_FOLDER_HANDLE = 2L
        const val OTHER_FOLDER_HANDLE = 3L
        const val RUBBISH_HANDLE = 4L
        const val PHOTO_HANDLE = 10L
        const val OTHER_PHOTO_HANDLE = 11L

        val folderId = NodeId(FOLDER_HANDLE)
    }
}
