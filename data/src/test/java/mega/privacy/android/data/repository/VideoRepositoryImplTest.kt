package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.VideoCompressorGateway
import mega.privacy.android.domain.entity.VideoCompressionState
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.VideoRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(ExperimentalCoroutinesApi::class)
class VideoRepositoryImplTest {
    private lateinit var underTest: VideoRepository

    private val videoCompressorGateway = mock<VideoCompressorGateway>()
    private val ioDispatcher = UnconfinedTestDispatcher()

    @BeforeAll
    fun setUp() {
        underTest = VideoRepositoryImpl(
            videoCompressorGateway = videoCompressorGateway,
            ioDispatcher = ioDispatcher,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            videoCompressorGateway,
        )
    }

    @Test
    fun `test that starting video compression emits events in order`() {
        runTest {
            val list = listOf(0.25f, 0.5f, 0.57f, 1f)
            val flow = flow {
                list.forEach {
                    emit(
                        VideoCompressionState.Progress(
                            progress = it,
                            currentIndex = 1,
                            totalCount = 2,
                            path = "",
                        )
                    )
                }
                emit(
                    VideoCompressionState.FinishedCompression(
                        returnedFile = "",
                        isSuccess = true,
                        messageId = 1,
                    )
                )
                emit(VideoCompressionState.Finished)
            }

            whenever(videoCompressorGateway.start()).thenReturn(flow)
            underTest.compressVideo(
                root = "",
                quality = VideoQuality.MEDIUM,
                filePath = "filePath",
                newFilePath = "newFilePath",
            ).test {
                list.forEach {
                    val item = awaitItem()
                    Truth.assertThat(item.javaClass)
                        .isEqualTo(VideoCompressionState.Progress::class.java)
                    Truth.assertThat((item as VideoCompressionState.Progress).progress)
                        .isEqualTo(it)
                }
                val finishedCompressionItem = awaitItem()
                Truth.assertThat(finishedCompressionItem.javaClass)
                    .isEqualTo(VideoCompressionState.FinishedCompression::class.java)
                val finished = awaitItem()
                Truth.assertThat(finished.javaClass)
                    .isEqualTo(VideoCompressionState.Finished::class.java)

                cancelAndConsumeRemainingEvents()
            }
        }
    }
}
