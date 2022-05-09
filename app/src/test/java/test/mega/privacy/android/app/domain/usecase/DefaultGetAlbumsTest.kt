@file:OptIn(FlowPreview::class)

package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.entity.Album
import mega.privacy.android.app.domain.entity.FavouriteInfo
import mega.privacy.android.app.domain.repository.AlbumsRepository
import mega.privacy.android.app.domain.usecase.DefaultGetAlbums
import mega.privacy.android.app.domain.usecase.GetAlbums
import mega.privacy.android.app.domain.usecase.GetAllFavorites
import mega.privacy.android.app.domain.usecase.GetThumbnail
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetAlbumsTest {
    private lateinit var underTest: GetAlbums

    private val getAllFavorites = mock<GetAllFavorites>()

    private val getThumbnail = mock<GetThumbnail>()

    private val cameraUploadFolderId = 10L
    private val mediaUploadFolderId = 20L

    private val albumsRepository = mock<AlbumsRepository> {
        on { getCameraUploadFolderId() }.thenReturn(cameraUploadFolderId)
        on { getMediaUploadFolderId() }.thenReturn(mediaUploadFolderId)
    }

    @Before
    fun setUp() {
        underTest = DefaultGetAlbums(
            getAllFavorites = getAllFavorites,
            getThumbnail = getThumbnail,
            albumsRepository = albumsRepository
        )
    }

    @Test
    fun `test that a favourite album is returned`() = runTest {
        underTest()
            .flatMapConcat { it.asFlow() }
            .test {
                assertThat(awaitItem()).isInstanceOf(Album.FavouriteAlbum::class.java)
                awaitComplete()
            }
    }

    @Test
    fun `test that only image or video thumbnails are used for the favourite album cover`() =
        runTest {
            val favouriteImage = createFavouriteItem(
                node = mock<MegaNode> {
                    on { isImage() }.thenReturn(false)
                    on { isVideo() }.thenReturn(false)
                },
                parentId = 2L
            )
            val thumbnail = File("ExpectedThumbnail")

            whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
            whenever(getThumbnail(any())).thenReturn(thumbnail)

            underTest()
                .flatMapConcat { it.asFlow() }
                .test {
                    assertThat(awaitItem().thumbnail).isNull()
                    awaitComplete()
                }
        }

    @Test
    fun `test that favourite image thumbnail is used as cover if present`() = runTest {
        val node = mock<MegaNode> { on { isImage() }.thenReturn(true) }
        val favouriteImage = createFavouriteItem(node = node)
        val thumbnail = File("ExpectedThumbnail")

        whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
        whenever(getThumbnail(any())).thenReturn(thumbnail)

        underTest()
            .flatMapConcat { it.asFlow() }
            .test {
                assertThat(awaitItem().thumbnail).isSameInstanceAs(thumbnail)
                awaitComplete()
            }
    }

    @Test
    fun `test that favourite video thumbnail is used if it is in the camera upload folder`() =
        runTest {
            val parentId = cameraUploadFolderId
            val node = mock<MegaNode> { on { isVideo() }.thenReturn(true) }
            val favouriteImage = createFavouriteItem(
                node = node,
                parentId = parentId
            )
            val thumbnail = File("ExpectedThumbnail")

            whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
            whenever(getThumbnail(any())).thenReturn(thumbnail)

            underTest()
                .flatMapConcat { it.asFlow() }
                .test {
                    assertThat(awaitItem().thumbnail).isSameInstanceAs(thumbnail)
                    awaitComplete()
                }
        }

    @Test
    fun `test that favourite video thumbnail is used if it is in the media upload folder`() =
        runTest {
            val parentId = mediaUploadFolderId
            val node = mock<MegaNode> { on { isVideo() }.thenReturn(true) }
            val favouriteImage = createFavouriteItem(
                node = node,
                parentId = parentId
            )
            val thumbnail = File("ExpectedThumbnail")

            whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
            whenever(getThumbnail(any())).thenReturn(thumbnail)

            underTest()
                .flatMapConcat { it.asFlow() }
                .test {
                    assertThat(awaitItem().thumbnail).isSameInstanceAs(thumbnail)
                    awaitComplete()
                }
        }


    @Test
    fun `test that only latest favourite thumbnail is used for favourite album cover`() = runTest {
        val node = mock<MegaNode> { on { isImage() }.thenReturn(true) }
        val favouriteImage1 = createFavouriteItem(id = 1, node = node, lastModified = 1)
        val favouriteImage2 = createFavouriteItem(id = 2, node = node, lastModified = 2)
        val favouriteImage3 = createFavouriteItem(id = 3, node = node, lastModified = 3)
        val favouriteImage4 = createFavouriteItem(id = 4, node = node, lastModified = 4)


        whenever(getAllFavorites()).thenReturn(
            flowOf(
                listOf(
                    favouriteImage2,
                    favouriteImage4,
                    favouriteImage1,
                    favouriteImage3,
                )
            )
        )

        val expectedThumbnail = File("ExpectedThumbnail")
        val thumbnail = File("OtherThumbnail")

        whenever(getThumbnail(argWhere { it != 4L })).thenReturn(thumbnail)
        whenever(getThumbnail(4L)).thenReturn(expectedThumbnail)

        underTest()
            .flatMapConcat { it.asFlow() }
            .test {
                assertThat(awaitItem().thumbnail).isSameInstanceAs(expectedThumbnail)
                awaitComplete()
            }
    }

    private fun createFavouriteItem(
        node: MegaNode,
        id: Long = 1L,
        parentId: Long = 2L,
        lastModified: Long = 3L,
    ): FavouriteInfo {
        val favouriteImage = FavouriteInfo(
            id = id,
            parentId = parentId,
            base64Id = "",
            modificationTime = lastModified,
            node = node,
            hasVersion = false,
            numChildFolders = 0,
            numChildFiles = 0
        )
        return favouriteImage
    }

}
