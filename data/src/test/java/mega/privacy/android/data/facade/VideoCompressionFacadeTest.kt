package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.FileGateway
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.domain.entity.VideoAttachment
import mega.privacy.android.domain.entity.VideoCompressionState
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalContracts
class VideoCompressionFacadeTest {

    private lateinit var underTest: VideoCompressorGateway
    private val fileGateway = mock<FileGateway>()
    private val videoAttachments by lazy {
        listOf(compressPrimaryVideo, secondaryVideo)
    }

    private val compressPrimaryVideo = VideoAttachment(
        originalPath = "/path/to/original/1",
        newPath = "path/to/new/1",
        id = 1,
        pendingMessageId = null
    )

    private val secondaryVideo = VideoAttachment(
        originalPath = "/path/to/original/2",
        newPath = "path/to/new/2",
        id = 2,
        pendingMessageId = null
    )

    @Before
    fun setUp() {
        underTest = VideoCompressionFacade(fileGateway)
    }

    @Test
    fun `test that compression ends when video attachments are empty`() = runTest {
        underTest.start().test {
            val event = awaitItem()
            assertThat(event.javaClass).isEqualTo(VideoCompressionState.Finished::class.java)
            awaitComplete()
        }
        assertThat(underTest.isRunning()).isFalse()
    }

    @Test
    fun `test that insufficient storage event is emitted when output roots is not set`() =
        runTest {
            underTest.addItems(videoAttachments)
            underTest.start().test {
                val event = awaitItem()
                assertThat(event.javaClass)
                    .isEqualTo(VideoCompressionState.InsufficientStorage::class.java)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun `test that insufficient storage event is emitted when there is not enough storage`() =
        runTest {
            underTest.setOutputRoot("/path/to/root")
            underTest.addItems(videoAttachments)
            whenever(fileGateway.hasEnoughStorage(any(), any())).thenReturn(false)
            underTest.start().test {
                val event = awaitItem()
                assertThat(event.javaClass)
                    .isEqualTo(VideoCompressionState.InsufficientStorage::class.java)
                cancelAndConsumeRemainingEvents()
            }
        }
}
