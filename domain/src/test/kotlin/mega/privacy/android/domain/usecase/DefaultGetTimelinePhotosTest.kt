package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetTimelinePhotosTest {
    private lateinit var underTest: GetTimelinePhotos

    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetTimelinePhotos(photosRepository = photosRepository)
    }

    @Test
    fun `test that current photos are returned`() = runTest {
        val photo = mock<Photo>()
        val expected = listOf(photo)
        photosRepository.stub {
            onBlocking { searchMegaPhotos() }.thenReturn(expected)
            onBlocking { monitorNodeUpdates() }.thenReturn(emptyFlow())
        }

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that node update notification of type New causes the latest photos to be fetched`() =
        runTest {
            val expected = listOf<Photo>(mock())
            val update = NodeUpdate(
                mapOf(NodeId(1L) to listOf(NodeChanges.New))
            )
            photosRepository.stub {
                onBlocking { searchMegaPhotos() }.thenReturn(emptyList(), expected)
                onBlocking { monitorNodeUpdates() }.thenReturn(flowOf(update))
            }

            underTest().test {
                awaitItem()
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that node update notification of type Favourite causes the latest photos to be fetched`() =
        runTest {
            val expected = listOf<Photo>(mock())
            val update = NodeUpdate(
                mapOf(NodeId(1L) to listOf(NodeChanges.Favourite))
            )
            photosRepository.stub {
                onBlocking { searchMegaPhotos() }.thenReturn(emptyList(), expected)
                onBlocking { monitorNodeUpdates() }.thenReturn(flowOf(update))
            }

            underTest().test {
                awaitItem()
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that node update notification of type Attributes causes the latest photos to be fetched`() =
        runTest {
            val expected = listOf<Photo>(mock())
            val update = NodeUpdate(
                mapOf(NodeId(1L) to listOf(NodeChanges.Attributes))
            )
            photosRepository.stub {
                onBlocking { searchMegaPhotos() }.thenReturn(emptyList(), expected)
                onBlocking { monitorNodeUpdates() }.thenReturn(flowOf(update))
            }

            underTest().test {
                awaitItem()
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that node update notification of type Parent causes the latest photos to be fetched`() =
        runTest {
            val expected = listOf<Photo>(mock())
            val update = NodeUpdate(
                mapOf(NodeId(1L) to listOf(NodeChanges.Parent))
            )
            photosRepository.stub {
                onBlocking { searchMegaPhotos() }.thenReturn(emptyList(), expected)
                onBlocking { monitorNodeUpdates() }.thenReturn(flowOf(update))
            }

            underTest().test {
                awaitItem()
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

}