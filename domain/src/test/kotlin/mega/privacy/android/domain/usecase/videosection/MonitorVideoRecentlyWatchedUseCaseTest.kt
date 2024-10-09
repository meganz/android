package mega.privacy.android.domain.usecase.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorVideoRecentlyWatchedUseCaseTest {
    private lateinit var underTest: MonitorVideoRecentlyWatchedUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorVideoRecentlyWatchedUseCase(videoSectionRepository)
    }

    @BeforeEach
    fun resetMock() {
        reset(videoSectionRepository)
    }

    @Test
    fun `test that getRecentlyWatchedVideoNodes function returns as expected`() = runTest {
        val testVideos = listOf(mock<TypedVideoNode>())
        whenever(videoSectionRepository.monitorRecentlyWatchedVideoNodes()).thenReturn(
            flowOf(
                testVideos
            )
        )
        underTest().test {
            assertThat(awaitItem()).isEqualTo(testVideos)
            cancelAndConsumeRemainingEvents()
        }
    }
}