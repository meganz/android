package mega.privacy.android.feature.photos.presentation.videos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.NodeSortConfigurationUiMapper
import mega.privacy.android.core.nodecomponents.mapper.NodeSourceTypeToViewTypeMapper
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.core.nodecomponents.model.NodeSortOption
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.Offline
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeUpdate
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedVideoNode
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.node.GetNodeContentUriByHandleUseCase
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.node.sort.MonitorSortCloudOrderUseCase
import mega.privacy.android.domain.usecase.offline.MonitorOfflineNodeUpdatesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.videosection.GetAllVideosUseCase
import mega.privacy.android.domain.usecase.videosection.GetSyncUploadsFolderIdsUseCase
import mega.privacy.android.feature.photos.mapper.VideoUiEntityMapper
import mega.privacy.android.feature.photos.presentation.videos.model.LocationFilterOption
import mega.privacy.android.feature.photos.presentation.videos.model.VideoUiEntity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class VideosTabViewModelTest {
    private lateinit var underTest: VideosTabViewModel

    private val getAllVideosUseCase = mock<GetAllVideosUseCase>()
    private val videoUiEntityMapper = mock<VideoUiEntityMapper>()
    private val monitorNodeUpdatesUseCase = mock<MonitorNodeUpdatesUseCase>()
    private val monitorOfflineNodeUpdatesUseCase = mock<MonitorOfflineNodeUpdatesUseCase>()
    private val getSyncUploadsFolderIdsUseCase = mock<GetSyncUploadsFolderIdsUseCase>()
    private val setCloudSortOrderUseCase = mock<SetCloudSortOrder>()
    private val monitorSortCloudOrderUseCase = mock<MonitorSortCloudOrderUseCase>()
    private val getNodeContentUriByHandleUseCase = mock<GetNodeContentUriByHandleUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val nodeSortConfigurationUiMapper = NodeSortConfigurationUiMapper()
    private val expectedId = NodeId(1L)
    private val expectedVideo = VideoUiEntity(
        id = expectedId,
        name = "video name",
        parentId = NodeId(50L),
        elementID = 1L,
        size = 100L,
        fileTypeInfo = VideoFileTypeInfo("video", "mp4", 1.minutes),
        duration = 1.minutes,
        locations = listOf(LocationFilterOption.AllLocations)
    )

    private val sortOrderFlow = MutableStateFlow(SortOrder.ORDER_MODIFICATION_DESC)

    private val syncUploadsFolderIds = listOf(100L, 200L)

    @BeforeEach
    fun setUp() {
        underTest = VideosTabViewModel(
            getAllVideosUseCase = getAllVideosUseCase,
            videoUiEntityMapper = videoUiEntityMapper,
            monitorNodeUpdatesUseCase = monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase = monitorOfflineNodeUpdatesUseCase,
            getSyncUploadsFolderIdsUseCase = getSyncUploadsFolderIdsUseCase,
            setCloudSortOrderUseCase = setCloudSortOrderUseCase,
            nodeSortConfigurationUiMapper = nodeSortConfigurationUiMapper,
            monitorSortCloudOrderUseCase = monitorSortCloudOrderUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            nodeSourceTypeToViewTypeMapper = NodeSourceTypeToViewTypeMapper(),
            getNodeContentUriByHandleUseCase = getNodeContentUriByHandleUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getAllVideosUseCase,
            videoUiEntityMapper,
            monitorNodeUpdatesUseCase,
            monitorOfflineNodeUpdatesUseCase,
            getSyncUploadsFolderIdsUseCase,
            setCloudSortOrderUseCase,
            monitorSortCloudOrderUseCase,
            getNodeContentUriByHandleUseCase,
            monitorShowHiddenItemsUseCase,
            monitorHiddenNodesEnabledUseCase,
        )

    }

    @Test
    fun `test that the initial state is loading`() = runTest {
        monitorNodeUpdatesUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        monitorOfflineNodeUpdatesUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        monitorSortCloudOrderUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        getAllVideosUseCase.stub {
            onBlocking { invoke() } doReturn emptyList()
        }
        monitorHiddenNodesEnabledUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        monitorShowHiddenItemsUseCase.stub {
            onBlocking { invoke() } doReturn flow { awaitCancellation() }
        }
        getSyncUploadsFolderIdsUseCase.stub {
            onBlocking { invoke() } doReturn emptyList()
        }
        underTest.uiState.test {
            assertThat(awaitItem()).isInstanceOf(VideosTabUiState.Loading::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that Data state is returned if values found`() = runTest {
        stubInitialValues()
        underTest.uiState.test {
            val actual = awaitItem() as? VideosTabUiState.Data
            if (actual != null) {
                assertThat(actual).isInstanceOf(VideosTabUiState.Data::class.java)
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.allVideoEntities.size).isEqualTo(2)
                assertThat(actual.selectedTypedNodes).isEmpty()
                assertThat(actual.query).isNull()
                assertThat(actual.highlightText).isEmpty()
                assertThat(actual.selectedSortConfiguration).isEqualTo(
                    nodeSortConfigurationUiMapper(
                        sortOrderFlow.value
                    )
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }


    @Test
    fun `test that getAllVideosUseCase is invoked when monitorNodeUpdatesUseCase is triggered`() =
        runTest {
            val testNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(VideoFileTypeInfo("video", "mp4", 1.minutes))
            }
            stubInitialValues()
            val nodeUpdateFlow = MutableStateFlow(NodeUpdate(emptyMap()))

            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.allVideoEntities).isNotEmpty()
                    clearInvocations(getAllVideosUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verify(getAllVideosUseCase).invoke(anyOrNull(), anyOrNull(), anyOrNull())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getAllVideosUseCase should not be invoked when no video nodes updated`() =
        runTest {
            val testNode = mock<FileNode> {
                on { id }.thenReturn(NodeId(1L))
                on { type }.thenReturn(TextFileTypeInfo("document", "txt"))
            }
            stubInitialValues()
            val nodeUpdateFlow = MutableStateFlow(NodeUpdate(emptyMap()))

            monitorNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(nodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.allVideoEntities).isNotEmpty()
                    clearInvocations(getAllVideosUseCase)

                    nodeUpdateFlow.emit(NodeUpdate(mapOf(testNode to emptyList())))
                    verifyNoInteractions(getAllVideosUseCase)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getAllVideosUseCase is invoked when monitorOfflineNodeUpdatesUseCase is triggered`() =
        runTest {
            val testOffline = mock<Offline> {
                on { id }.thenReturn(1)
            }
            stubInitialValues()
            val offlineNodeUpdateFlow = MutableStateFlow(emptyList<Offline>())

            monitorOfflineNodeUpdatesUseCase.stub {
                on { invoke() }.thenReturn(offlineNodeUpdateFlow)
            }

            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.allVideoEntities).isNotEmpty()
                    clearInvocations(getAllVideosUseCase)

                    offlineNodeUpdateFlow.emit(listOf(testOffline))
                    verify(getAllVideosUseCase).invoke(anyOrNull(), anyOrNull(), anyOrNull())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that getAllVideosUseCase is invoked when monitorSortCloudOrderUseCase is triggered`() =
        runTest {
            stubInitialValues()
            val sortCloudOrderFlow = MutableStateFlow<SortOrder?>(null)

            monitorSortCloudOrderUseCase.stub {
                on { invoke() }.thenReturn(sortCloudOrderFlow)
            }

            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.allVideoEntities).isNotEmpty()
                    clearInvocations(getAllVideosUseCase)

                    sortCloudOrderFlow.emit(SortOrder.ORDER_FAV_ASC)
                    verify(getAllVideosUseCase).invoke(anyOrNull(), anyOrNull(), anyOrNull())
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that uiState is correctly updated when searchQuery is invoked`() = runTest {
        val query = "query"
        val video = createVideoUiEntity(handle = 2L, name = "video file in query")
        val typedNode = mock<TypedVideoNode>()

        stubInitialValues()
        // Set up default mock for initial state (no query)
        whenever(
            getAllVideosUseCase(
                searchQuery = anyOrNull(),
                tag = anyOrNull(),
                description = anyOrNull()
            )
        ).thenReturn(emptyList())

        // Set up specific mock for search query - must be after the general one
        whenever(
            getAllVideosUseCase(
                searchQuery = query,
                tag = query.removePrefix("#"),
                description = query
            )
        ).thenReturn(listOf(typedNode))
        whenever(videoUiEntityMapper(eq(typedNode), anyOrNull())).thenReturn(video)


        // First subscription to get initial state and ensure clean state
        underTest.uiState
            .filterIsInstance<VideosTabUiState.Data>()
            .test {
                val initial = awaitItem()
                // Verify initial state has no query
                assertThat(initial.query).isNull()
                underTest.searchQuery(query)

                val actual = awaitItem()

                assertThat(actual.query).isEqualTo(query)
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.allVideoEntities.size).isEqualTo(1)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that uiState is correctly updated when setCloudSortOrder is invoked`() =
        runTest {
            val initialConfiguration = nodeSortConfigurationUiMapper(sortOrderFlow.value)

            stubInitialValues()
            val newConfiguration = NodeSortConfiguration(
                NodeSortOption.Modified,
                SortDirection.Ascending
            )


            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    val initial = awaitItem()
                    assertThat(initial.allVideoEntities).isNotEmpty()
                    assertThat(initial.allVideoEntities.size).isEqualTo(2)
                    assertThat(initial.selectedSortConfiguration).isEqualTo(initialConfiguration)

                    underTest.setCloudSortOrder(newConfiguration)

                    val actual = awaitItem()
                    assertThat(actual.allVideoEntities).isNotEmpty()
                    assertThat(actual.allVideoEntities.size).isEqualTo(2)
                    assertThat(actual.selectedSortConfiguration).isEqualTo(newConfiguration)

                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that selectedTypedNodes are updated correctly`() =
        runTest {
            val video1 = createVideoUiEntity(handle = 1L)
            val video2 = createVideoUiEntity(handle = 2L)
            stubInitialValues(
                nodesAndEntities = mapOf(
                    createTypedVideoNode(video1) to video1,
                    createTypedVideoNode(video2) to video2
                )
            )

            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    skipItems(1)
                    underTest.onItemLongClicked(video1)
                    var actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).hasSize(1)
                    assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(video1.id)

                    underTest.onItemClicked(video2)
                    actual = awaitItem()
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
            stubInitialValues(
                nodesAndEntities = mapOf(
                    createTypedVideoNode(video1) to video1,
                    createTypedVideoNode(video2) to video2
                )
            )

            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    var actual = awaitItem()
                    assertThat(actual.allVideoEntities).isNotEmpty()
                    assertThat(actual.selectedTypedNodes).isEmpty()

                    underTest.selectAllVideos()
                    actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).hasSize(2)
                    assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(
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
            stubInitialValues(
                nodesAndEntities = mapOf(
                    createTypedVideoNode(video1) to video1,
                    createTypedVideoNode(video2) to video2
                )
            )

            underTest.uiState
                .filterIsInstance<VideosTabUiState.Data>()
                .test {
                    awaitItem()
                    underTest.onItemLongClicked(video1)
                    var actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).hasSize(1)
                    assertThat(actual.selectedTypedNodes.map { it.id }).containsExactly(video1.id)

                    underTest.clearSelection()
                    actual = awaitItem()
                    assertThat(actual.selectedTypedNodes).isEmpty()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @ParameterizedTest(name = "when the showHiddenItems is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that uiState is updated correctly`(
        showHiddenItems: Boolean,
    ) = runTest {

        val video1 = createVideoUiEntity(handle = 1L, isSensitiveInherited = true)
        val video2 = createVideoUiEntity(handle = 2L, isMarkedSensitive = true)
        val video3 = createVideoUiEntity(handle = 3L)
        val video4 = createVideoUiEntity(handle = 4L)
        stubInitialValues(
            nodesAndEntities = mapOf(
                createTypedVideoNode(video1) to video1,
                createTypedVideoNode(video2) to video2,
                createTypedVideoNode(video3) to video3,
                createTypedVideoNode(video4) to video4,
            )
        )

        whenever(monitorShowHiddenItemsUseCase()).thenReturn(
            flow {
                emit(showHiddenItems)
                awaitCancellation()
            }
        )

        underTest.uiState
            .filterIsInstance<VideosTabUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.showHiddenItems).isEqualTo(showHiddenItems)
                assertThat(actual.allVideoEntities).hasSize(
                    if (showHiddenItems) {
                        4
                    } else {
                        2
                    }
                )
                if (showHiddenItems) {
                    assertThat(actual.allVideoEntities.map { it.id }).containsExactly(
                        video1.id,
                        video2.id,
                        video3.id,
                        video4.id
                    )
                } else {
                    assertThat(actual.allVideoEntities.map { it.id }).containsExactly(
                        video3.id,
                        video4.id
                    )
                }
                cancelAndIgnoreRemainingEvents()
            }
    }

    @ParameterizedTest(name = " and monitorHiddenNodesEnabledUseCase {0}")
    @ValueSource(booleans = [true, false])
    fun `test that case that showHiddenItems is updated correctly`(
        hiddenNodeEnabled: Boolean,
    ) = runTest {
        stubInitialValues()
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(
            flow {
                emit(hiddenNodeEnabled)
                awaitCancellation()
            }
        )

        underTest.uiState
            .filterIsInstance<VideosTabUiState.Data>()
            .test {
                val actual = awaitItem()
                assertThat(actual.allVideoEntities).isNotEmpty()
                assertThat(actual.showHiddenItems).isEqualTo(!hiddenNodeEnabled)
                cancelAndIgnoreRemainingEvents()
            }
    }

    private suspend fun stubInitialValues(
        nodesAndEntities: Map<TypedVideoNode, VideoUiEntity> = mapOf(
            mock<TypedVideoNode>() to expectedVideo,
            mock<TypedVideoNode>() to expectedVideo
        ),
    ) {
        whenever(monitorNodeUpdatesUseCase()).thenReturn(
            flow {
                emit(NodeUpdate(emptyMap()))
                awaitCancellation()
            }
        )
        whenever(monitorOfflineNodeUpdatesUseCase()).thenReturn(
            flow {
                emit(emptyList())
                awaitCancellation()
            }
        )

        setCloudSortOrderUseCase.stub {
            onBlocking { invoke(any()) }.thenAnswer { invocation -> sortOrderFlow.tryEmit(invocation.arguments[0] as SortOrder) }
        }

        whenever(monitorSortCloudOrderUseCase()).thenReturn(
            sortOrderFlow
        )
        whenever(
            getAllVideosUseCase(
                searchQuery = anyOrNull(),
                tag = anyOrNull(),
                description = anyOrNull()
            )
        ).thenReturn(nodesAndEntities.keys.toList())

        whenever(
            videoUiEntityMapper(
                any(),
                anyOrNull()
            )
        ).thenAnswer { invocation -> nodesAndEntities[invocation.arguments[0]] }

        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(
            flow {
                emit(true)
                awaitCancellation()
            }
        )
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(
            flow {
                emit(false)
                awaitCancellation()
            }
        )
        // Set up default syncUploadsFolderIds for all tests
        whenever(getSyncUploadsFolderIdsUseCase()).thenReturn(syncUploadsFolderIds)
    }

    private fun createVideoUiEntity(
        handle: Long,
        parentHandle: Long = 50L,
        name: String = "video name $handle",
        duration: Duration = 1.minutes,
        isSharedItems: Boolean = false,
        isMarkedSensitive: Boolean = false,
        isSensitiveInherited: Boolean = false,
    ) = VideoUiEntity(
        id = NodeId(handle),
        name = name,
        parentId = NodeId(parentHandle),
        elementID = 1L,
        duration = duration,
        isSharedItems = isSharedItems,
        size = 100L,
        fileTypeInfo = VideoFileTypeInfo("video", "mp4", duration),
        isMarkedSensitive = isMarkedSensitive,
        isSensitiveInherited = isSensitiveInherited,
        locations = listOf(LocationFilterOption.AllLocations)
    )

    private fun createTypedVideoNode(
        videoUiEntity: VideoUiEntity,
    ) = TypedVideoNode(
        fileNode = mock<FileNode> {
            on { id }.thenReturn(videoUiEntity.id)
            on { name }.thenReturn(videoUiEntity.name)
            on { parentId }.thenReturn(videoUiEntity.parentId)
            on { isMarkedSensitive }.thenReturn(videoUiEntity.isMarkedSensitive)
            on { isSensitiveInherited }.thenReturn(videoUiEntity.isSensitiveInherited)
        },
        duration = videoUiEntity.duration,
        elementID = null,
        isOutShared = videoUiEntity.isSharedItems,
        watchedTimestamp = 0L,
        collectionTitle = null,
    )
}