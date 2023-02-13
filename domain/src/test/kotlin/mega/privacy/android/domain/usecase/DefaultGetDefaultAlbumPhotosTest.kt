package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetDefaultAlbumPhotosTest {

    lateinit var underTest: GetDefaultAlbumPhotos
    private val photosRepository = mock<PhotosRepository>()
    private val nodeRepository = mock<NodeRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetDefaultAlbumPhotos(photosRepository, nodeRepository)
    }

    @Test
    fun `test that nodes are not updated when node update type is not in monitored types`() =
        runTest {
            whenever(photosRepository.searchMegaPhotos()).thenReturn(listOf(
                createVideo(id = 1L)
            ))
            val node = mock<Node>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(
                flowOf(
                    NodeUpdate(
                        mapOf(
                            node to NodeChanges.values().filter {
                                it !in listOf(
                                    NodeChanges.New,
                                    NodeChanges.Favourite,
                                    NodeChanges.Attributes,
                                    NodeChanges.Parent,
                                )
                            }
                        )
                    )
                )
            )

            underTest(listOf())
                .test {
                    awaitItem()
                    awaitComplete()
                }
        }

    @Test
    fun `test that nodes are updated when node update of type NodeChanges New is returned`() =
        runTest {
            whenever(photosRepository.searchMegaPhotos()).thenReturn(listOf(
                createVideo(id = 1L)
            ))
            val node = mock<Node>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(mapOf(
                node to listOf(NodeChanges.New)))))

            underTest(listOf())
                .test {
                    awaitItem()
                    awaitItem()
                    awaitComplete()
                }
        }

    @Test
    fun `test that nodes are updated when node update of type NodeChanges Favourite is returned`() =
        runTest {
            whenever(photosRepository.searchMegaPhotos()).thenReturn(listOf(
                createVideo(id = 1L)
            ))
            val node = mock<Node>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(mapOf(
                node to listOf(NodeChanges.Favourite)))))

            underTest(listOf())
                .test {
                    awaitItem()
                    awaitItem()
                    awaitComplete()
                }
        }

    @Test
    fun `test that nodes are updated when node update of type NodeChanges Attributes is returned`() =
        runTest {
            whenever(photosRepository.searchMegaPhotos()).thenReturn(listOf(
                createVideo(id = 1L)
            ))
            val node = mock<Node>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(mapOf(
                node to listOf(NodeChanges.Attributes)))))

            underTest(listOf())
                .test {
                    awaitItem()
                    awaitItem()
                    awaitComplete()
                }
        }

    @Test
    fun `test that nodes are updated when node update of type NodeChanges Parent is returned`() =
        runTest {
            whenever(photosRepository.searchMegaPhotos()).thenReturn(listOf(
                createVideo(id = 1L)
            ))
            val node = mock<Node>()
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(mapOf(
                node to listOf(NodeChanges.Parent)))))

            underTest(listOf())
                .test {
                    awaitItem()
                    awaitItem()
                    awaitComplete()
                }
        }

    private fun createVideo(
        id: Long,
        parentId: Long = 0L,
        isFavourite: Boolean = false,
        modificationTime: LocalDateTime = LocalDateTime.now(),
    ): Photo {
        return Photo.Video(
            id = id,
            parentId = parentId,
            name = "",
            isFavourite = isFavourite,
            creationTime = LocalDateTime.now(),
            modificationTime = modificationTime,
            thumbnailFilePath = "thumbnailFilePath",
            previewFilePath = "previewFilePath",
            fileTypeInfo = VideoFileTypeInfo("", "", duration = 123)
        )
    }
}
