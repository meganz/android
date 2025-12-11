package mega.privacy.android.feature.photos.presentation.videos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetSyncUploadsFolderIdsUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.feature.photos.presentation.videos.model.DurationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideosTabViewModelTest {
    private lateinit var underTest: VideosTabViewModel

    private val getAllVideosUseCase = mock<GetAllVideosUseCase>()
    private val videoUiEntityMapper = mock<VideoUiEntityMapper>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val getSyncUploadsFolderIdsUseCase = mock<GetSyncUploadsFolderIdsUseCase>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val nodeSortConfigurationUiMapper = mock<NodeSortConfigurationUiMapper>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()

    private val expectedId = NodeId(1L)
    private val expectedVideo = mock<VideoUiEntity> {
        on { id }.thenReturn(expectedId)
        on { name }.thenReturn("video name")
        on { elementID }.thenReturn(1L)
    }

    private val syncUploadsFolderIds = listOf(100L, 200L)

    @BeforeEach
    fun setUp() {
        runBlocking {
            whenever(monitorNodeUpdatesUseCase()).thenReturn(
                flow {
                    emptyMap<FileNode, NodeUpdate>()
                    awaitCancellation()
                }
            )
            whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
                flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            )
            whenever(monitorSortCloudOrderUseCase()).thenReturn(
                flow {
                    emit(null)
                    awaitCancellation()
                }
            )

            whenever(getCloudSortOrder()).thenReturn(SortOrder.ORDER_MODIFICATION_DESC)
            whenever(
                getAllVideosUseCase(
                    searchQuery = anyOrNull(),
                    tag = anyOrNull(),
                    description = anyOrNull()
                )
            ).thenReturn(listOf(mock(), mock()))
            whenever(videoUiEntityMapper(any())).thenReturn(expectedVideo)
            whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(
                NodeSortConfiguration.default
            )
        }
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = VideosTabViewModel(
            getAllVideosUseCase = getAllVideosUseCase,
            videoUiEntityMapper = videoUiEntityMapper,
            getCloudSortOrder = getCloudSortOrder,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            getSyncUploadsFolderIdsUseCase = getSyncUploadsFolderIdsUseCase,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllVideosUseCase,
            videoUiEntityMapper,
            getCloudSortOrder,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            getSyncUploadsFolderIdsUseCase,
            setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase
        )
    }

    @Test
    fun `test that the initial state is correctly updated`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(VideosTabUiState.Loading::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState is correctly updated triggerRefresh is invoked`() =
        runTest {
            underTest.triggerRefresh()

            underTest.uiState.drop(1).test {
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideoEntities.size).isEqualTo(2)
                assertThat(actual.selectedTypedNodes).isEmpty()
                assertThat(actual.query).isNull()
                assertThat(actual.highlightText).isEmpty()
                assertThat(actual.selectedSortConfiguration).isEqualTo(NodeSortConfiguration.default)
                assertThat(actual.locationSelectedFilterOption).isEqualTo(LocationFilterOption.AllLocations)
                assertThat(actual.durationSelectedFilterOption).isEqualTo(DurationFilterOption.AllDurations)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            val testFileNode = mock<FileNode> {
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 10.seconds))
            }
            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(NodeUpdate(mapOf(testFileNode to emptyList())))
                        awaitCancellation()
                    }
                )
            }

            underTest.uiState.drop(1).test {
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideoEntities.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when monitorNodeUpdatesUseCase is triggered but changed node is not videoType`() =
        runTest {
            val testFileNode = mock<FileNode> {
                on { type }.thenReturn(TextFileTypeInfo("TextFile", "txt"))
            }
            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(NodeUpdate(mapOf(testFileNode to emptyList())))
                        awaitCancellation()
                    }
                )
            }

            underTest.uiState.test {
                assertThat(awaitItem()).isInstanceOf(VideosTabUiState.Loading::class.java)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when monitorOfflineNodeUpdatesUseCase is triggered`() =
        runTest {
            val testOffline = mock<Offline>()
            monitorOfflineNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(listOf(testOffline))
                        awaitCancellation()
                    }
                )
            }

            underTest.uiState.drop(1).test {
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideoEntities.size).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState is correctly updated when searchQuery is invoked`() = runTest {
        val query = "query"
        val video = createVideoUiEntity(handle = 2L, name = "video file in query")
        val typedNode = mock<TypedVideoNode>()
        whenever(
            getAllVideosUseCase(
                searchQuery = query,
                tag = query.removePrefix("#"),
                description = query
            )
        ).thenReturn(listOf(typedNode))
        whenever(videoUiEntityMapper(typedNode)).thenReturn(video)
        underTest.searchQuery(query)

        underTest.uiState.drop(1).test {
            val actual = awaitItem() as VideosTabUiState.Data
            assertThat(actual.allVideoEntities).isNotEmpty()
            assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
            assertThat(actual.allVideoEntities.size).isEqualTo(1)
            assertThat(actual.query).isEqualTo(query)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState is correctly updated when setCloudSortOrder is invoked`() =
        runTest {
            val sortOrder = SortOrder.ORDER_FAV_ASC
            whenever(nodeSortConfigurationUiMapper(any<NodeSortConfiguration>()))
                .thenReturn(sortOrder)
            whenever(nodeSortConfigurationUiMapper(any(), any())).thenReturn(
                NodeSortConfiguration.default
            )

            underTest.setCloudSortOrder(NodeSortConfiguration.default)

            underTest.uiState.test {
                verify(setCloudSortOrderUseCase).invoke(any())
                assertThat(awaitItem()).isInstanceOf(VideosTabUiState.Loading::class.java)
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.sortOrder).isEqualTo(SortOrder.ORDER_MODIFICATION_DESC)
                assertThat(actual.allVideoEntities.size).isEqualTo(2)
                assertThat(actual.selectedSortConfiguration).isEqualTo(NodeSortConfiguration.default)
                cancelAndIgnoreRemainingEvents()
            }
        }


    @ParameterizedTest(name = "by {0}")
    @MethodSource("provideLocationOptions")
    fun `test that setLocationSelectedFilterOption filters videos`(
        locationFilterOption: LocationFilterOption,
    ) =
        runTest {
            val cloudDriveVideo = createVideoUiEntity(handle = 1L, parentHandle = 50L)
            val cameraUploadsVideo = createVideoUiEntity(handle = 2L, parentHandle = 100L)
            val sharedVideo =
                createVideoUiEntity(handle = 3L, parentHandle = 50L, isSharedItems = true)

            val cloudDriveList = listOf(cloudDriveVideo)
            val cameraUploadsList = listOf(cameraUploadsVideo)
            val sharedList = listOf(sharedVideo)

            val list = cloudDriveList + cameraUploadsList + sharedList

            initVideosForFilter(videos = list)
            underTest.setLocationSelectedFilterOption(locationFilterOption)
            advanceUntilIdle()

            underTest.uiState.test {
                // Wait for initial Loading state
                val loadingState = awaitItem()
                assertThat(loadingState).isInstanceOf(VideosTabUiState.Loading::class.java)

                // Then wait for the updated Data state with filtered results
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.locationSelectedFilterOption).isEqualTo(locationFilterOption)
                assertThat(actual.allVideoEntities).hasSize(
                    when (locationFilterOption) {
                        LocationFilterOption.AllLocations -> list.size
                        LocationFilterOption.CloudDrive -> (cloudDriveList + sharedList).size
                        LocationFilterOption.CameraUploads -> cameraUploadsList.size
                        LocationFilterOption.SharedItems -> sharedList.size
                    }
                )
                assertThat(actual.allVideoEntities.map { it.id }).isEqualTo(
                    when (locationFilterOption) {
                        LocationFilterOption.AllLocations -> list
                        LocationFilterOption.CloudDrive -> cloudDriveList + sharedList
                        LocationFilterOption.CameraUploads -> cameraUploadsList
                        LocationFilterOption.SharedItems -> sharedList
                    }.map { it.id }
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun provideLocationOptions(): List<LocationFilterOption> =
        listOf(
            LocationFilterOption.AllLocations,
            LocationFilterOption.CloudDrive,
            LocationFilterOption.CameraUploads,
            LocationFilterOption.SharedItems
        )

    @ParameterizedTest(name = "by {0}")
    @MethodSource("provideDurationOptions")
    fun `test that setDurationSelectedFilterOption filters videos`(
        durationFilterOption: DurationFilterOption,
    ) =
        runTest {
            val video1Seconds = createVideoUiEntity(handle = 1L, duration = 1.seconds)
            val video5Seconds = createVideoUiEntity(handle = 2L, duration = 5.seconds)
            val video10Seconds = createVideoUiEntity(handle = 3L, duration = 10.seconds)
            val video30Seconds = createVideoUiEntity(handle = 4L, duration = 30.seconds)
            val video60Seconds = createVideoUiEntity(handle = 5L, duration = 60.seconds)
            val video2Minutes = createVideoUiEntity(handle = 6L, duration = 2.minutes)
            val video4Minutes = createVideoUiEntity(handle = 7L, duration = 4.minutes)
            val video8Minutes = createVideoUiEntity(handle = 8L, duration = 8.minutes)
            val video15Minutes = createVideoUiEntity(handle = 9L, duration = 15.minutes)
            val video20Minutes = createVideoUiEntity(handle = 10L, duration = 20.minutes)
            val video25Minutes = createVideoUiEntity(handle = 11L, duration = 25.minutes)
            val video1Hour = createVideoUiEntity(handle = 12L, duration = 1.hours)
            val video10Hour = createVideoUiEntity(handle = 13L, duration = 10.hours)

            val lessThan10Seconds = listOf(video1Seconds, video5Seconds)
            val between10And60Seconds = listOf(video10Seconds, video30Seconds, video60Seconds)
            val between1And4 = listOf(video2Minutes, video4Minutes)
            val between4And20 = listOf(video8Minutes, video15Minutes, video20Minutes)
            val moreThan20 = listOf(video25Minutes, video1Hour, video10Hour)

            val allVideos =
                lessThan10Seconds + between10And60Seconds + between1And4 + between4And20 + moreThan20

            initVideosForFilter(allVideos)
            underTest.setDurationSelectedFilterOption(durationFilterOption)
            advanceUntilIdle()

            underTest.uiState.test {
                // Wait for initial Loading state
                val loadingState = awaitItem()
                assertThat(loadingState).isInstanceOf(VideosTabUiState.Loading::class.java)

                // Then wait for the updated Data state with filtered results
                val actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.durationSelectedFilterOption).isEqualTo(durationFilterOption)
                assertThat(actual.allVideoEntities).hasSize(
                    when (durationFilterOption) {
                        DurationFilterOption.AllDurations -> allVideos.size
                        DurationFilterOption.LessThan10Seconds -> lessThan10Seconds.size
                        DurationFilterOption.Between10And60Seconds -> between10And60Seconds.size
                        DurationFilterOption.Between1And4 -> between1And4.size
                        DurationFilterOption.Between4And20 -> between4And20.size
                        DurationFilterOption.MoreThan20 -> moreThan20.size
                    }
                )
                assertThat(actual.allVideoEntities.map { it.id }).isEqualTo(
                    when (durationFilterOption) {
                        DurationFilterOption.AllDurations -> allVideos
                        DurationFilterOption.LessThan10Seconds -> lessThan10Seconds
                        DurationFilterOption.Between10And60Seconds -> between10And60Seconds
                        DurationFilterOption.Between1And4 -> between1And4
                        DurationFilterOption.Between4And20 -> between4And20
                        DurationFilterOption.MoreThan20 -> moreThan20
                    }.map { it.id }
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun provideDurationOptions(): List<DurationFilterOption> =
        listOf(
            DurationFilterOption.AllDurations,
            DurationFilterOption.LessThan10Seconds,
            DurationFilterOption.Between10And60Seconds,
            DurationFilterOption.Between1And4,
            DurationFilterOption.Between4And20,
            DurationFilterOption.MoreThan20
        )

    @Test
    fun `test that selectedTypedNodes are updated correctly`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L)
            val video2 = createVideoUiEntity(handle = 2L)

            initVideosForFilter(videos = listOf(video1, video2))

            underTest.onItemLongClicked(video1)

            underTest.uiState.drop(1).test {
                var actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.selectedTypedNodes).hasSize(1)
                assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(video1.id)

                underTest.onItemClicked(video2)
                actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.selectedTypedNodes).hasSize(2)
                assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(
                    video1.id,
                    video2.id
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after selectAllVideos is invoked`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L)
            val video2 = createVideoUiEntity(handle = 2L)

            initVideosForFilter(videos = listOf(video1, video2))

            underTest.uiState.test {
                // Wait for initial Loading state
                val loadingState = awaitItem()
                assertThat(loadingState).isInstanceOf(VideosTabUiState.Loading::class.java)

                // Wait for initial Data state
                val initialDataState = awaitItem() as VideosTabUiState.Data
                assertThat(initialDataState.allVideoEntities).isNotEmpty()
                assertThat(initialDataState.selectedTypedNodes).isEmpty()

                underTest.selectAllVideos()
                advanceUntilIdle()

                val updatedState = awaitItem() as VideosTabUiState.Data
                assertThat(updatedState.selectedTypedNodes).hasSize(2)
                assertThat(updatedState.selectedTypedNodes.map { it.id }).containsExactly(
                    video1.id,
                    video2.id
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly after clearSelection is invoked`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L)
            val video2 = createVideoUiEntity(handle = 2L)

            initVideosForFilter(videos = listOf(video1, video2))

            underTest.onItemLongClicked(video1)

            underTest.uiState.drop(1).test {
                var actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.selectedTypedNodes).hasSize(1)
                assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(video1.id)

                underTest.clearSelection()
                actual = awaitItem() as VideosTabUiState.Data
                assertThat(actual.selectedTypedNodes).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun createVideoUiEntity(
        handle: Long,
        parentHandle: Long = 50L,
        name: String = "video name $handle",
        duration: Duration = 1.minutes,
        isSharedItems: Boolean = false,
    ) = VideoUiEntity(
        id = NodeId(handle),
        name = name,
        parentId = NodeId(parentHandle),
        elementID = 1L,
        duration = duration,
        isSharedItems = isSharedItems,
        size = 100L,
        fileTypeInfo = VideoFileTypeInfo("video", "mp4", duration),
    )

    private fun createTypedVideoNode(
        videoUiEntity: VideoUiEntity
    ) = mock<TypedVideoNode> {
        on { id }.thenReturn(videoUiEntity.id)
        on { name }.thenReturn(videoUiEntity.name)
        on { parentId }.thenReturn(videoUiEntity.parentId)
        on { duration }.thenReturn(videoUiEntity.duration)
        on { isOutShared }.thenReturn(videoUiEntity.isSharedItems)
    }

    private suspend fun initVideosForFilter(
        videos: List<VideoUiEntity>,
        syncUploadsFolderIds: List<Long> = this.syncUploadsFolderIds,
    ) {
        val nodes = videos.map { createTypedVideoNode(it) }

        whenever(
            getAllVideosUseCase(
                searchQuery = anyOrNull(),
                tag = anyOrNull(),
                description = anyOrNull()
            )
        ).thenReturn(nodes)

        nodes.map { node ->
            val video = videos.first { node.id == it.id }
            whenever(videoUiEntityMapper(node)).thenReturn(video)
        }
        whenever(getSyncUploadsFolderIdsUseCase()).thenReturn(syncUploadsFolderIds)
    }
}