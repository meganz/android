package mega.privacy.android.domain.usecase.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.VideoNode
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.repository.VideoSectionRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetAllVideosUseCaseTest {
    private lateinit var underTest: GetAllVideosUseCase
    private val videoSectionRepository = mock<VideoSectionRepository>()
    private val nodeRepository = mock<NodeRepository>()

    val order = SortOrder.ORDER_MODIFICATION_DESC

    @BeforeAll
    fun setUp() {
        underTest = GetAllVideosUseCase(
            videoSectionRepository = videoSectionRepository,
            nodeRepository = nodeRepository
        )
    }

    @BeforeEach
    fun resetMock() {
        reset(
            videoSectionRepository,
            nodeRepository
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that videos is not empty`() = runTest {
        val list = listOf(mock<VideoNode>())
        whenever(videoSectionRepository.getAllVideos(order)).thenReturn(list)
        whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(emptyMap())))
        underTest(order).test {
            assertThat(awaitItem()).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that video is empty`() {
        runTest {
            whenever(videoSectionRepository.getAllVideos(order)).thenReturn(emptyList())
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(emptyMap())))
            underTest(order).test {
                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Test
    fun `test that video returns result of getAllVideos when a node update occur`() =
        runTest {
            whenever(videoSectionRepository.getAllVideos(order)).thenReturn(emptyList())
            whenever(nodeRepository.monitorNodeUpdates()).thenReturn(flowOf(NodeUpdate(emptyMap())))
            assertThat(underTest(order).count()).isEqualTo(2)
        }
}