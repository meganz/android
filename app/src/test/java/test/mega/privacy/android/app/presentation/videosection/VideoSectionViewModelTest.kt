package test.mega.privacy.android.app.presentation.videosection

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.app.domain.usecase.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.app.presentation.videosection.VideoSectionViewModel
import mega.privacy.android.app.presentation.videosection.mapper.UIVideoMapper
import mega.privacy.android.app.presentation.videosection.model.UIVideo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetFileUrlByNodeHandleUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.file.GetFingerprintUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerIsRunningUseCase
import mega.privacy.android.domain.usecase.mediaplayer.MegaApiHttpServerStartUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoSectionViewModelTest {
    private lateinit var underTest: VideoSectionViewModel

    private val getAllVideosUseCase = mock<GetAllVideosUseCase>()
    private val uiVideoMapper = mock<UIVideoMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val getFingerprintUseCase = mock<GetFingerprintUseCase>()
    private val megaApiHttpServerIsRunningUseCase = mock<MegaApiHttpServerIsRunningUseCase>()
    private val megaApiHttpServerStartUseCase = mock<MegaApiHttpServerStartUseCase>()
    private val getFileUrlByNodeHandleUseCase = mock<GetFileUrlByNodeHandleUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        wheneverBlocking { monitorNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        wheneverBlocking { monitorOfflineNodeUpdatesUseCase() }.thenReturn(emptyFlow())
        underTest = VideoSectionViewModel(
            getAllVideosUseCase = getAllVideosUseCase,
            uiVideoMapper = uiVideoMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            getNodeByHandle = getNodeByHandle,
            getFingerprintUseCase = getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase = megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase = megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase = getFileUrlByNodeHandleUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllVideosUseCase,
            uiVideoMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            getNodeByHandle,
            getFingerprintUseCase,
            megaApiHttpServerIsRunningUseCase,
            megaApiHttpServerStartUseCase,
            getFileUrlByNodeHandleUseCase,
            getNodeByIdUseCase
        )
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
            assertThat(initial.isPendingRefresh).isFalse()
            assertThat(initial.sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(initial.progressBarShowing).isEqualTo(true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the videos are retrieved when the nodes are refreshed`() = runTest {
        val expectedVideo: UIVideo = mock {
            on { name }.thenReturn("video name")
        }

        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

        whenever(getAllVideosUseCase()).thenReturn(
            listOf(mock(), mock())
        )
        whenever(uiVideoMapper(any())).thenReturn(expectedVideo)

        underTest.state.test(200) {
            val initial = awaitItem()
            assertThat(initial.allVideos).isEmpty()
            assertThat(initial.progressBarShowing).isEqualTo(true)
            assertThat(initial.scrollToTop).isEqualTo(false)

            underTest.refreshNodes()

            val actual = awaitItem()
            assertThat(actual.allVideos).isNotEmpty()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allVideos.size).isEqualTo(2)
            assertThat(actual.progressBarShowing).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the sortOrder is updated when order is changed`() = runTest {
        whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

        whenever(getAllVideosUseCase()).thenReturn(emptyList())
        underTest.refreshWhenOrderChanged()
        underTest.state.test(200) {
            assertThat(awaitItem().sortOrder).isEqualTo(SortOrder.ORDER_NONE)
            assertThat(awaitItem().sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test when when item is long clicked, then it updates selected item by 1`() =
        runTest {
            val expectedVideo: UIVideo = mock {
                on { name }.thenReturn("video name")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

            whenever(getAllVideosUseCase()).thenReturn(
                listOf(mock(), mock())
            )
            whenever(uiVideoMapper(any())).thenReturn(expectedVideo)

            underTest.state.test(200) {
                assertThat(awaitItem().allVideos).isEmpty()
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos).isNotEmpty()
                underTest.onLongItemClicked(expectedVideo, 0)
                assertThat(awaitItem().selectedVideoHandles.size).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when selected item gets clicked then checked index gets incremented by 1`() =
        runTest {
            val expectedVideo: UIVideo = mock {
                on { name }.thenReturn("video name")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

            whenever(getAllVideosUseCase()).thenReturn(
                listOf(mock(), mock())
            )
            whenever(uiVideoMapper(any())).thenReturn(expectedVideo)

            underTest.state.test(200) {
                assertThat(awaitItem().allVideos).isEmpty()
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos).isNotEmpty()
                underTest.onLongItemClicked(expectedVideo, 0)
                assertThat(awaitItem().selectedVideoHandles.size).isEqualTo(1)
                underTest.onItemClicked(expectedVideo, 1)
                assertThat(awaitItem().selectedVideoHandles.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when select all videos clicked size of video items and equal to size of selected videos`() =
        runTest {
            val expectedVideo: UIVideo = mock {
                on { name }.thenReturn("video name")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

            whenever(getAllVideosUseCase()).thenReturn(
                listOf(mock(), mock())
            )
            whenever(uiVideoMapper(any())).thenReturn(expectedVideo)

            underTest.state.test(200) {
                assertThat(awaitItem().allVideos).isEmpty()
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos).isNotEmpty()
                underTest.selectAllNodes()
                val actual = awaitItem()
                assertThat(actual.selectedVideoHandles.size).isEqualTo(actual.selectedVideoHandles.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when clear all videos clicked, size of selected videos is empty`() =
        runTest {
            val expectedVideo: UIVideo = mock {
                on { name }.thenReturn("video name")
            }

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)

            whenever(getAllVideosUseCase()).thenReturn(
                listOf(mock(), mock())
            )
            whenever(uiVideoMapper(any())).thenReturn(expectedVideo)

            underTest.state.test(200) {
                assertThat(awaitItem().allVideos).isEmpty()
                underTest.refreshNodes()
                assertThat(awaitItem().allVideos).isNotEmpty()
                underTest.clearAllSelectedVideos()
                assertThat(awaitItem().selectedVideoHandles.isEmpty()).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
