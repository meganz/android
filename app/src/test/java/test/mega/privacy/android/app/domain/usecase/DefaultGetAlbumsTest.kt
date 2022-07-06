package test.mega.privacy.android.app.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.usecase.DefaultGetAlbums
import mega.privacy.android.domain.entity.Album
import mega.privacy.android.domain.entity.FavouriteInfo
import mega.privacy.android.domain.repository.AlbumsRepository
import mega.privacy.android.domain.usecase.GetAlbums
import mega.privacy.android.domain.usecase.GetAllFavorites
import mega.privacy.android.domain.usecase.GetThumbnail
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import org.mockito.kotlin.clearInvocations
import java.io.File

@OptIn(FlowPreview::class)
@ExperimentalCoroutinesApi
class DefaultGetAlbumsTest {
    private lateinit var underTest: GetAlbums

    val flow = flow<List<FavouriteInfo>> {
        emit(emptyList())
        awaitCancellation()
    }
    private val getAllFavorites = mock<GetAllFavorites> {
        on { invoke() }.thenReturn(
            flow
        )
    }

    private val getThumbnail = mock<GetThumbnail>()

    private val cameraUploadFolderId = 10L
    private val mediaUploadFolderId = 20L

    private val albumsRepository = mock<AlbumsRepository> {
        onBlocking { getCameraUploadFolderId() }.thenReturn(cameraUploadFolderId)
        onBlocking { getMediaUploadFolderId() }.thenReturn(mediaUploadFolderId)
    }

    @Before
    fun setUp() {
        underTest = DefaultGetAlbums(
            getAllFavorites = getAllFavorites,
            albumsRepository = albumsRepository,
            getThumbnail = getThumbnail
        )
    }

    @Test
    fun `test that a favourite album is returned`() = runTest {
        underTest()
            .flatMapConcat { it.asFlow() }
            .test {
                assertThat(awaitItem()).isInstanceOf(Album.FavouriteAlbum::class.java)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that only image or video thumbnails are used for the favourite album cover`() =
        runTest {
            val nonImageFavouriteItem = createFavouriteItem()
            val thumbnail = File("NotExpectedThumbnail")

            whenever(getAllFavorites()).thenReturn(flowOf(listOf(nonImageFavouriteItem)))
            whenever(getThumbnail(any())).thenReturn(thumbnail)

            underTest()
                .flatMapConcat { it.asFlow() }
                .test {
                    assertThat(awaitItem().thumbnail).isNull()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that favourite image thumbnail is used as cover if present`() = runTest {
        val nodeId = 34L
        val favouriteImage = createFavouriteItem(id = nodeId, isImage = true)
        val thumbnail = File("ExpectedThumbnail")

        whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
        whenever(getThumbnail(nodeId)).thenReturn(thumbnail)

        underTest()
            .flatMapConcat { it.asFlow() }
            .drop(1)
            .test {
                assertThat(awaitItem().thumbnail).isSameInstanceAs(thumbnail)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that favourite video thumbnail is used if it is in the camera upload folder`() =
        runTest {
            val parentId = cameraUploadFolderId
            val favouriteImage = createFavouriteItem(
                parentId = parentId,
                isVideo = true
            )
            val thumbnail = File("ExpectedThumbnail")

            whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
            whenever(getThumbnail(any())).thenReturn(thumbnail)

            underTest()
                .flatMapConcat { it.asFlow() }
                .drop(1)
                .test {
                    assertThat(awaitItem().thumbnail).isSameInstanceAs(thumbnail)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that favourite video thumbnail is used if it is in the media upload folder`() =
        runTest {
            val parentId = mediaUploadFolderId
            val favouriteImage = createFavouriteItem(
                parentId = parentId,
                isVideo = true
            )
            val thumbnail = File("ExpectedThumbnail")

            whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
            whenever(getThumbnail(any())).thenReturn(thumbnail)

            underTest()
                .flatMapConcat { it.asFlow() }
                .drop(1)
                .test {
                    assertThat(awaitItem().thumbnail).isSameInstanceAs(thumbnail)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that video is not used for the thumbnail if not in camera upload or media folder`() =
        runTest {
            val favouriteImage = createFavouriteItem(
                isVideo = true
            )
            val thumbnail = File("NotExpectedThumbnail")

            whenever(getAllFavorites()).thenReturn(flowOf(listOf(favouriteImage)))
            whenever(getThumbnail(any())).thenReturn(thumbnail)

            underTest()
                .flatMapConcat { it.asFlow() }
                .test {
                    assertThat(awaitItem().thumbnail).isNull()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that only latest favourite thumbnail is used for favourite album cover`() = runTest {
        val favouriteImage1 = createFavouriteItem(id = 1, lastModified = 1, isImage = true)
        val favouriteImage2 = createFavouriteItem(id = 2, lastModified = 2, isImage = true)
        val favouriteImage3 = createFavouriteItem(id = 3, lastModified = 3, isImage = true)
        val favouriteImage4 = createFavouriteItem(id = 4, lastModified = 4, isImage = true)

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

        whenever(getThumbnail(any())).thenReturn(thumbnail)
        whenever(getThumbnail(4L)).thenReturn(expectedThumbnail)

        underTest()
            .flatMapConcat { it.asFlow() }
            .drop(1)
            .test {
                assertThat(awaitItem().thumbnail).isSameInstanceAs(expectedThumbnail)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that favourite album item count matches number of images`() = runTest {

        val favourites = (1..6).map {
            createFavouriteItem(id = it.toLong(), lastModified = it.toLong(), isImage = true)
        }

        whenever(getAllFavorites()).thenReturn(
            flowOf(
                favourites
            )
        )

        val thumbnail = File("Thumbnail")

        whenever(getThumbnail(any())).thenReturn(thumbnail)

        underTest()
            .flatMapConcat { it.asFlow() }
            .drop(1) // drop the default empty favourite album
            .test {
                assertThat(awaitItem().itemCount).isEqualTo(favourites.size)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that items are fetched again when new favourites are returned`() = runTest {
        clearInvocations(getThumbnail)
        val favourites = (1..6).map {
            createFavouriteItem(id = it.toLong(), lastModified = it.toLong(), isImage = true)
        }

        whenever(getAllFavorites()).thenReturn(
            flowOf(
                favourites,
                favourites
            )
        )

        underTest().test {
            cancelAndConsumeRemainingEvents()
        }

        verify(getThumbnail, times(2)).invoke(any())
    }

    // black box test
    @Test
    fun `test that new album is return when new favourites are returned`() = runTest {
        val id1 = 1L
        val id2 = 2L
        val thumbnail1 = File("Thumbnail1")
        val thumbnail2 = File("Thumbnail2")

        whenever(getAllFavorites()).thenReturn(
            flowOf(
                listOf(
                    createFavouriteItem(id = id1, isImage = true)
                ),
                listOf(
                    createFavouriteItem(id = id2, isImage = true)
                ),
            )
        )

        whenever(getThumbnail(id1)).thenReturn(thumbnail1)
        whenever(getThumbnail(id2)).thenReturn(thumbnail2)

        underTest().test {
            assertThat(awaitItem()[0].thumbnail).isNull()
            assertThat(awaitItem()[0].thumbnail).isEqualTo(thumbnail1)
            assertThat(awaitItem()[0].thumbnail).isEqualTo(thumbnail2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createFavouriteItem(
        id: Long = 1L,
        parentId: Long = 2L,
        lastModified: Long = 3L,
        isImage: Boolean = false,
        isVideo: Boolean = false,
    ): FavouriteInfo {
        return FavouriteInfo(
            id = id,
            name = "testName",
            size = 1000L,
            label = MegaNode.NODE_LBL_RED,
            parentId = parentId,
            base64Id = "",
            modificationTime = lastModified,
            hasVersion = false,
            numChildFolders = 0,
            numChildFiles = 0,
            isImage = isImage,
            isVideo = isVideo,
            isFolder = true,
            isFavourite = true,
            isExported = false,
            isTakenDown = false,
        )
    }

}
