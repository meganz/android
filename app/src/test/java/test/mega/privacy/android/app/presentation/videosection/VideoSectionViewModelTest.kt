package test.mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.VideoNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoSectionViewModelTest {
    private lateinit var underTest: VideoSectionViewModel

    private val getAllVideosUseCase = mock<GetAllVideosUseCase>()
    private val uiVideoMapper = mock<UIVideoMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        underTest = VideoSectionViewModel(
            getAllVideosUseCase = getAllVideosUseCase,
            uiVideoMapper = uiVideoMapper,
            getCloudSortOrder = getCloudSortOrder
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(getAllVideosUseCase, uiVideoMapper, getCloudSortOrder)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test(200) {
            val initial = awaitItem()
            assertThat(initial.allVideos).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getting videos is returned`() = runTest {
        val firstExpectedHandle: Long = 1234567
        val secondExpectedHandle: Long = 7654321
        val thirdExpectedHandle: Long = 13571234
        val firstExpectedVideo = getVideoNode(firstExpectedHandle)
        val secondExpectedVideo = getVideoNode(secondExpectedHandle)
        val thirdExpectedVideo = getVideoNode(thirdExpectedHandle)

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

        whenever(getAllVideosUseCase(SortOrder.ORDER_MODIFICATION_DESC)).thenReturn(
            flowOf(
                listOf(firstExpectedVideo, secondExpectedVideo, thirdExpectedVideo)
            )
        )
        underTest.state.test(200) {
            assertThat(awaitItem().allVideos).isEmpty()
            val actual = awaitItem()
            assertThat(actual.allVideos).isNotEmpty()
            assertThat(actual.allVideos.size).isEqualTo(3)
        }
    }

    private fun getVideoNode(handle: Long): VideoNode {
        val expectedFileNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(handle))
            on { name }.thenReturn("video file name")
            on { size }.thenReturn(100)
        }
        return mock {
            on { fileNode }.thenReturn(expectedFileNode)
            on { duration }.thenReturn(100)
            on { thumbnailFilePath }.thenReturn("video file thumbnail")
        }
    }
}