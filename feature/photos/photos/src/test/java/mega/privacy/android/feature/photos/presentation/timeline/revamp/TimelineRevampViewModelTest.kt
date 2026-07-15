package mega.privacy.android.feature.photos.presentation.timeline.revamp

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestExtension
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import kotlinx.coroutines.test.advanceUntilIdle
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.media.MediaTimelineFilter
import mega.privacy.android.domain.entity.media.MediaTimelineSection
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.GetNodeListByIdsUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetCameraUploadFolderHandlesUseCase
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetMediaTimelineSectionsUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.ListMediaNodesByOffsetUseCase
import mega.privacy.android.domain.usecase.photos.MonitorMediaNodeContentChangesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelineGridSizeUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineGridSizeUseCase
import mega.privacy.android.domain.usecase.photos.SignalMediaCountChangesUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.MediaType
import mega.privacy.android.feature.photos.model.PhotosNodeContentItemV2
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.TimelineFilterUiState
import mega.privacy.android.feature.photos.presentation.timeline.TimelineTabSortOptions
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature.photos.presentation.timeline.revamp.mapper.MediaTimelineNodeUiItemMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeCompactSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeDefaultSelectedEvent
import mega.privacy.mobile.analytics.event.MediaScreenGridSizeLargeSelectedEvent
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class TimelineRevampViewModelTest {

    private lateinit var underTest: TimelineRevampViewModel

    private val getMediaTimelineSectionsUseCase = mock<GetMediaTimelineSectionsUseCase>()
    private val listMediaNodesByOffsetUseCase = mock<ListMediaNodesByOffsetUseCase>()
    private val mediaTimelineNodeUiItemMapper = mock<MediaTimelineNodeUiItemMapper>()
    private val getTimelineFilterPreferencesUseCase = mock<GetTimelineFilterPreferencesUseCase>()
    private val setTimelineFilterPreferencesUseCase = mock<SetTimelineFilterPreferencesUseCase>()
    private val monitorTimelineGridSizeUseCase = mock<MonitorTimelineGridSizeUseCase>()
    private val setTimelineGridSizeUseCase = mock<SetTimelineGridSizeUseCase>()
    private val timelineFilterUiStateMapper = mock<TimelineFilterUiStateMapper>()
    private val getCameraUploadFolderHandlesUseCase = mock<GetCameraUploadFolderHandlesUseCase>()
    private val monitorHiddenNodesEnabledUseCase = mock<MonitorHiddenNodesEnabledUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val getNodeListByIdsUseCase = mock<GetNodeListByIdsUseCase>()
    private val signalMediaCountChangesUseCase = mock<SignalMediaCountChangesUseCase>()
    private val monitorMediaNodeContentChangesUseCase =
        mock<MonitorMediaNodeContentChangesUseCase>()
    private val gridSizePreference = MutableStateFlow<Int?>(null)

    private suspend fun initUnderTest(
        filterUiState: TimelineFilterUiState = TimelineFilterUiState(),
        isHiddenNodesEnabled: Boolean = false,
        showHiddenItems: Boolean = false,
        mediaChanges: Flow<Unit> = emptyFlow(),
        contentChanges: Flow<Set<Long>> = emptyFlow(),
    ) {
        whenever(timelineFilterUiStateMapper(anyOrNull<Map<String, String?>>(), any()))
            .thenReturn(filterUiState)
        whenever(timelineFilterUiStateMapper(any<TimelineFilterUiState>(), any()))
            .thenReturn(DEFAULT_FILTER)
        whenever(getCameraUploadFolderHandlesUseCase()).thenReturn(emptyList())
        whenever(monitorHiddenNodesEnabledUseCase()).thenReturn(flowOf(isHiddenNodesEnabled))
        whenever(monitorShowHiddenItemsUseCase()).thenReturn(flowOf(showHiddenItems))
        whenever(signalMediaCountChangesUseCase()).thenReturn(mediaChanges)
        whenever(monitorMediaNodeContentChangesUseCase()).thenReturn(contentChanges)
        whenever(monitorTimelineGridSizeUseCase()).thenReturn(gridSizePreference)
        whenever(setTimelineGridSizeUseCase(any())) doSuspendableAnswer {
            gridSizePreference.value = it.getArgument(0)
        }
        underTest = TimelineRevampViewModel(
            getMediaTimelineSectionsUseCase = getMediaTimelineSectionsUseCase,
            listMediaNodesByOffsetUseCase = listMediaNodesByOffsetUseCase,
            mediaTimelineNodeUiItemMapper = mediaTimelineNodeUiItemMapper,
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase = setTimelineFilterPreferencesUseCase,
            monitorTimelineGridSizeUseCase = monitorTimelineGridSizeUseCase,
            setTimelineGridSizeUseCase = setTimelineGridSizeUseCase,
            timelineFilterUiStateMapper = timelineFilterUiStateMapper,
            getCameraUploadFolderHandlesUseCase = getCameraUploadFolderHandlesUseCase,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            getNodeListByIdsUseCase = getNodeListByIdsUseCase,
            signalMediaCountChangesUseCase = signalMediaCountChangesUseCase,
            monitorMediaNodeContentChangesUseCase = monitorMediaNodeContentChangesUseCase,
        )
    }

    @Test
    fun `test that sections are emitted as Data with no loaded nodes initially`() = runTest {
        val sections = listOf(section(groupId = "May 2026", count = 5))
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(sections)
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            val data = awaitItem()
            assertThat(data.sections).isEqualTo(sections)
            assertThat(data.loadedNodes).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onVisibleRangeChanged loads and caches the visible nodes`() = runTest {
        val sections = listOf(section(groupId = "May 2026", count = 5))
        val nodes = (0 until 5).map { mock<TypedFileNode>() }
        val items = (0 until 5).map { item(id = it.toLong()) }
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(sections)
        whenever(
            listMediaNodesByOffsetUseCase(any(), any(), any(), any(), any())
        ).thenReturn(nodes)
        nodes.forEachIndexed { index, node ->
            whenever(mediaTimelineNodeUiItemMapper(node)).thenReturn(items[index])
        }
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // initial Data with empty loadedNodes
            underTest.onVisibleRangeChanged(firstIndex = 0, lastIndex = 4)
            val loaded = awaitItem()
            assertThat(loaded.loadedNodes.keys).containsExactly(0, 1, 2, 3, 4)
            assertThat(loaded.loadedNodes[0]).isEqualTo(items[0])
            cancelAndIgnoreRemainingEvents()
        }

        verify(listMediaNodesByOffsetUseCase).invoke(
            filter = any(),
            section = any(),
            order = eq(SortOrder.ORDER_MODIFICATION_DESC),
            maxElements = eq(5),
            offset = eq(0L),
        )
    }

    @Test
    fun `test that uiState emits Empty when sections are empty`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Empty>().test {
            assertThat(awaitItem()).isEqualTo(TimelineRevampUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that getMediaTimelineSectionsUseCase is invoked with the default filter`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        initUnderTest()

        underTest.uiState.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(getMediaTimelineSectionsUseCase)
            .invoke(DEFAULT_FILTER, SortOrder.ORDER_MODIFICATION_DESC)
    }

    @Test
    fun `test that listMediaNodesByOffsetUseCase is invoked with the default filter`() = runTest {
        val sections = listOf(section(groupId = "May 2026", count = 5))
        val nodes = (0 until 5).map { mock<TypedFileNode>() }
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(sections)
        whenever(
            listMediaNodesByOffsetUseCase(any(), any(), any(), any(), any())
        ).thenReturn(nodes)
        nodes.forEach { node ->
            whenever(mediaTimelineNodeUiItemMapper(node)).thenReturn(item(id = 0L))
        }
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem()
            underTest.onVisibleRangeChanged(firstIndex = 0, lastIndex = 4)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(listMediaNodesByOffsetUseCase).invoke(
            filter = eq(DEFAULT_FILTER),
            section = any(),
            order = any(),
            maxElements = any(),
            offset = any(),
        )
    }

    @Test
    fun `test that filterUiState reflects the mapped preferences`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        val expected = TimelineFilterUiState(
            isRemembered = true,
            mediaType = FilterMediaType.IMAGES,
            mediaSource = FilterMediaSource.CloudDrive,
        )
        initUnderTest(filterUiState = expected)

        underTest.filterUiState.test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onFilterChange persists the selected filter preferences`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        initUnderTest()

        underTest.onFilterChange(
            TimelineFilterRequest(
                isRemembered = true,
                mediaType = FilterMediaType.IMAGES,
                mediaSource = FilterMediaSource.CloudDrive,
            )
        )
        advanceUntilIdle()

        verify(setTimelineFilterPreferencesUseCase).invoke(
            mapOf(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "true",
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to FilterMediaType.IMAGES.toMediaTypeValue(),
                TimelinePreferencesJSON.JSON_KEY_LOCATION.value to FilterMediaSource.CloudDrive.toLocationValue(),
            )
        )
    }

    @Test
    fun `test that the filter ui state and resolved camera upload handles are passed to the mapper`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
            val filterUiState = TimelineFilterUiState(mediaSource = FilterMediaSource.CameraUpload)
            val handles = listOf(NodeId(PRIMARY_HANDLE), NodeId(SECONDARY_HANDLE))
            initUnderTest(filterUiState = filterUiState)
            whenever(getCameraUploadFolderHandlesUseCase()).thenReturn(handles)

            underTest.uiState.test {
                awaitItem()
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            verify(timelineFilterUiStateMapper).invoke(filterUiState, handles)
        }

    @Test
    fun `test that the timeline still loads when resolving camera upload handles fails`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any()))
                .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
            val filterUiState = TimelineFilterUiState(mediaSource = FilterMediaSource.CameraUpload)
            initUnderTest(filterUiState = filterUiState)
            whenever(getCameraUploadFolderHandlesUseCase()).thenThrow(RuntimeException("boom"))

            underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
                // The pipeline recovers instead of terminating, so Data is still produced.
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            // Handle resolution failed, so the filter is derived with no source scoping.
            verify(timelineFilterUiStateMapper).invoke(filterUiState, emptyList())
        }

    @Test
    fun `test that selectedTimePeriod defaults to All and updates on selection`() = runTest {
        initUnderTest()

        assertThat(underTest.selectedTimePeriod.value).isEqualTo(MediaTimePeriod.All)

        underTest.onMediaTimePeriodSelected(MediaTimePeriod.Months)

        assertThat(underTest.selectedTimePeriod.value).isEqualTo(MediaTimePeriod.Months)
    }

    @Test
    fun `test that uiState resets to Loading when the filter changes`() = runTest {
        val changedFilter = DEFAULT_FILTER.copy(category = MediaTimelineFilter.Category.Photos)
        initUnderTest()
        // Produce a distinct filter once the user changes the selection, so currentFilter actually
        // changes (null preferences -> default; an applied selection -> a different filter).
        whenever(timelineFilterUiStateMapper(anyOrNull<Map<String, String?>>(), any())).thenAnswer {
            if (it.getArgument<Map<String, String?>?>(0) == null) {
                TimelineFilterUiState()
            } else {
                TimelineFilterUiState(mediaType = FilterMediaType.IMAGES)
            }
        }
        whenever(timelineFilterUiStateMapper(any<TimelineFilterUiState>(), any())).thenAnswer {
            if (it.getArgument<TimelineFilterUiState>(0).mediaType == FilterMediaType.IMAGES) {
                changedFilter
            } else {
                DEFAULT_FILTER
            }
        }
        whenever(getMediaTimelineSectionsUseCase(eq(DEFAULT_FILTER), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        // The changed filter's sections never arrive, so the UI stays in Loading after the change.
        getMediaTimelineSectionsUseCase.stub {
            on {
                invoke(
                    eq(changedFilter),
                    any()
                )
            } doSuspendableAnswer { awaitCancellation() }
        }

        underTest.uiState.test {
            var state = awaitItem()
            while (state !is TimelineRevampUiState.Data) state = awaitItem()

            underTest.onFilterChange(
                TimelineFilterRequest(
                    isRemembered = false,
                    mediaType = FilterMediaType.IMAGES,
                    mediaSource = FilterMediaSource.AllPhotos,
                )
            )

            assertThat(awaitItem()).isEqualTo(TimelineRevampUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that sections are loaded with HideSensitive when hidden nodes are enabled and show-hidden is off`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
            initUnderTest(isHiddenNodesEnabled = true, showHiddenItems = false)

            underTest.uiState.test {
                awaitItem()
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            verify(getMediaTimelineSectionsUseCase).invoke(
                eq(DEFAULT_FILTER.copy(sensitivity = MediaTimelineFilter.Sensitivity.HideSensitive)),
                any(),
            )
        }

    @Test
    fun `test that sections are loaded with ShowAll when hidden nodes are enabled but show-hidden is on`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
            initUnderTest(isHiddenNodesEnabled = true, showHiddenItems = true)

            underTest.uiState.test {
                awaitItem()
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            verify(getMediaTimelineSectionsUseCase).invoke(
                eq(DEFAULT_FILTER.copy(sensitivity = MediaTimelineFilter.Sensitivity.ShowAll)),
                any(),
            )
        }

    @Test
    fun `test that uiState Data exposes whether hidden nodes are enabled`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest(isHiddenNodesEnabled = true, showHiddenItems = true)

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            assertThat(awaitItem().isHiddenNodesEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that uiState Data defaults the grid size to Default`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            assertThat(awaitItem().gridSize).isEqualTo(TimelineGridSize.Default)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onGridSizeChange updates the grid size in uiState`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem()
            underTest.onGridSizeChange(TimelineGridSize.Compact)
            assertThat(awaitItem().gridSize).isEqualTo(TimelineGridSize.Compact)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onGridSizeChange tracks the corresponding analytics event`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        initUnderTest()

        underTest.onGridSizeChange(TimelineGridSize.Compact)
        underTest.onGridSizeChange(TimelineGridSize.Default)
        underTest.onGridSizeChange(TimelineGridSize.Large)

        assertThat(analyticsExtension.events).containsAtLeast(
            MediaScreenGridSizeCompactSelectedEvent,
            MediaScreenGridSizeDefaultSelectedEvent,
            MediaScreenGridSizeLargeSelectedEvent,
        )
    }

    @Test
    fun `test that onZoomIn steps the grid towards a larger size`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // Default
            underTest.onZoomIn()
            // Default -> Large (larger tiles, fewer columns)
            assertThat(awaitItem().gridSize).isEqualTo(TimelineGridSize.Large)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onZoomOut steps the grid towards a smaller size`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // Default
            underTest.onZoomOut()
            // Default -> Compact (smaller tiles, more columns)
            assertThat(awaitItem().gridSize).isEqualTo(TimelineGridSize.Compact)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onZoomIn is a no-op when already at the largest size`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // Default
            underTest.onGridSizeChange(TimelineGridSize.Large)
            assertThat(awaitItem().gridSize).isEqualTo(TimelineGridSize.Large)
            underTest.onZoomIn()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onZoomOut is a no-op when already at the smallest size`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // Default
            underTest.onGridSizeChange(TimelineGridSize.Compact)
            assertThat(awaitItem().gridSize).isEqualTo(TimelineGridSize.Compact)
            underTest.onZoomOut()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that actionUiState isReady becomes true once the sections finish loading`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any()))
                .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
            initUnderTest()

            underTest.actionUiState.test {
                var state = awaitItem()
                while (!state.isReady) state = awaitItem()
                assertThat(state.isReady).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the sort action is disabled when there is no content`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        initUnderTest()

        underTest.actionUiState.test {
            awaitItem()
            advanceUntilIdle()
            underTest.updateSortActionEnablement(
                isEnableCameraUploadPageShowing = false,
                mediaSource = FilterMediaSource.AllPhotos,
            )
            var state = awaitItem()
            while (state.normalModeItem.enableSort) state = awaitItem()
            assertThat(state.normalModeItem.enableSort).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the sort action is disabled when the CU page is showing for a non Cloud Drive source`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any()))
                .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
            initUnderTest()

            underTest.actionUiState.test {
                awaitItem()
                advanceUntilIdle()
                underTest.updateSortActionEnablement(
                    isEnableCameraUploadPageShowing = true,
                    mediaSource = FilterMediaSource.CameraUpload,
                )
                var state = awaitItem()
                while (state.normalModeItem.enableSort) state = awaitItem()
                assertThat(state.normalModeItem.enableSort).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the sort action is re-enabled when content is present and the CU page is not showing`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any()))
                .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
            initUnderTest()

            underTest.actionUiState.test {
                awaitItem()
                advanceUntilIdle()
                // Disable first.
                underTest.updateSortActionEnablement(
                    isEnableCameraUploadPageShowing = true,
                    mediaSource = FilterMediaSource.CameraUpload,
                )
                var disabled = awaitItem()
                while (disabled.normalModeItem.enableSort) disabled = awaitItem()

                // Then re-enable.
                underTest.updateSortActionEnablement(
                    isEnableCameraUploadPageShowing = false,
                    mediaSource = FilterMediaSource.AllPhotos,
                )
                var enabled = awaitItem()
                while (!enabled.normalModeItem.enableSort) enabled = awaitItem()
                assertThat(enabled.normalModeItem.enableSort).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that updateSortActionBasedOnCUPageEnablement disables sort when the CU page is enabled for a non Cloud Drive source`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any()))
                .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
            initUnderTest()

            underTest.actionUiState.test {
                awaitItem()
                advanceUntilIdle()
                underTest.updateSortActionBasedOnCUPageEnablement(
                    isEnableCameraUploadPageShowing = false,
                    mediaSource = FilterMediaSource.CameraUpload,
                    isCUPageEnabled = true,
                )
                var state = awaitItem()
                while (state.normalModeItem.enableSort) state = awaitItem()
                assertThat(state.normalModeItem.enableSort).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uiState Data defaults the sort option to Newest`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            assertThat(awaitItem().currentSort).isEqualTo(TimelineTabSortOptions.Newest)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that onSortOptionsChange updates the sort option in uiState`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "May 2026", count = 3)))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem()
            underTest.onSortOptionsChange(TimelineTabSortOptions.Oldest)
            var data = awaitItem()
            while (data.currentSort != TimelineTabSortOptions.Oldest) data = awaitItem()
            assertThat(data.currentSort).isEqualTo(TimelineTabSortOptions.Oldest)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that changing sort to Oldest reloads the sections with ascending order`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
        initUnderTest()

        underTest.uiState.test {
            awaitItem()
            advanceUntilIdle()
            underTest.onSortOptionsChange(TimelineTabSortOptions.Oldest)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        verify(getMediaTimelineSectionsUseCase)
            .invoke(any(), eq(SortOrder.ORDER_MODIFICATION_ASC))
    }

    @Test
    fun `test that the per-section page query uses the selected sort order`() = runTest {
        val sections = listOf(section(groupId = "May 2026", count = 5))
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(sections)
        whenever(
            listMediaNodesByOffsetUseCase(any(), any(), any(), any(), any())
        ).thenReturn(emptyList())
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem()
            underTest.onSortOptionsChange(TimelineTabSortOptions.Oldest)
            advanceUntilIdle()
            underTest.onVisibleRangeChanged(firstIndex = 0, lastIndex = 4)
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        verify(listMediaNodesByOffsetUseCase).invoke(
            filter = any(),
            section = any(),
            order = eq(SortOrder.ORDER_MODIFICATION_ASC),
            maxElements = any(),
            offset = any(),
        )
    }

    @Test
    fun `test that selecting the Years period queries Year-granularity sections for the year cards`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
            initUnderTest()

            underTest.uiState.test {
                awaitItem()
                advanceUntilIdle()
                underTest.onMediaTimePeriodSelected(MediaTimePeriod.Years)
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            verify(getMediaTimelineSectionsUseCase).invoke(
                eq(DEFAULT_FILTER.copy(granularity = MediaTimelineFilter.Granularity.Year)),
                any(),
            )
        }

    @Test
    fun `test that selecting the Months period queries Month-granularity sections for the month cards`() =
        runTest {
            whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(emptyList())
            initUnderTest()

            underTest.uiState.test {
                awaitItem()
                advanceUntilIdle()
                underTest.onMediaTimePeriodSelected(MediaTimePeriod.Months)
                advanceUntilIdle()
                cancelAndIgnoreRemainingEvents()
            }

            verify(getMediaTimelineSectionsUseCase).invoke(
                eq(DEFAULT_FILTER.copy(granularity = MediaTimelineFilter.Granularity.Month)),
                any(),
            )
        }

    @Test
    fun `test that selecting the Years period exposes the year cards in uiState`() = runTest {
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(section(groupId = "2026", count = 5)))
        val fileType = StaticImageFileTypeInfo(mimeType = "image/jpeg", extension = "jpg")
        val node = mock<TypedFileNode> {
            on { modificationTime } doReturn 0L
            on { id } doReturn NodeId(1L)
            on { thumbnailPath } doReturn null
            on { previewPath } doReturn null
            on { type } doReturn fileType
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }
        whenever(listMediaNodesByOffsetUseCase(any(), any(), any(), any(), any()))
            .thenReturn(listOf(node))
        initUnderTest()

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            underTest.onMediaTimePeriodSelected(MediaTimePeriod.Years)
            var data = awaitItem()
            while (data.periodCards.isEmpty()) data = awaitItem()
            assertThat(data.selectedPeriod).isEqualTo(MediaTimePeriod.Years)
            assertThat(data.periodCards).hasSize(1)
            assertThat(data.periodCards.first().period).isEqualTo(PhotosNodeListCardPeriod.Year)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that retrieveTypedNodeFromSelection resolves ids to typed nodes and exposes them`() =
        runTest {
            val nodes = listOf(mock<TypedNode>(), mock<TypedNode>())
            whenever(getNodeListByIdsUseCase(any())).thenReturn(nodes)
            initUnderTest()

            val result = underTest.retrieveTypedNodeFromSelection(setOf(1L, 2L))

            assertThat(result).isEqualTo(nodes)
            assertThat(underTest.selectedPhotosInTypedNodesFlow.value).isEqualTo(nodes)
            verify(getNodeListByIdsUseCase).invoke(listOf(NodeId(1L), NodeId(2L)))
        }

    @Test
    fun `test that retrieveTypedNodeFromSelection clears the exposed nodes when selection is empty`() =
        runTest {
            whenever(getNodeListByIdsUseCase(any())).thenReturn(emptyList())
            initUnderTest()

            val result = underTest.retrieveTypedNodeFromSelection(emptySet())

            assertThat(result).isEmpty()
            assertThat(underTest.selectedPhotosInTypedNodesFlow.value).isEmpty()
        }

    @Test
    fun `test that retrieveTypedNodeFromSelection returns empty when the fetch fails`() = runTest {
        whenever(getNodeListByIdsUseCase(any())).thenThrow(RuntimeException("boom"))
        initUnderTest()

        val result = underTest.retrieveTypedNodeFromSelection(setOf(1L))

        assertThat(result).isEmpty()
    }

    @Test
    fun `test that a media change silently refreshes the sections without emitting Loading`() =
        runTest {
            val initial = listOf(section(groupId = "May 2026", count = 3))
            val refreshed = listOf(
                section(groupId = "Jun 2026", count = 2),
                section(groupId = "May 2026", count = 3),
            )
            whenever(getMediaTimelineSectionsUseCase(any(), any()))
                .thenReturn(initial, refreshed)
            val updates = Channel<Unit>(capacity = 1)
            initUnderTest(mediaChanges = updates.consumeAsFlow())

            underTest.uiState.test {
                var state = awaitItem()
                while (state !is TimelineRevampUiState.Data) state = awaitItem()
                assertThat((state as TimelineRevampUiState.Data).sections).isEqualTo(initial)

                updates.send(Unit)
                advanceUntilIdle()

                val next = awaitItem()
                // The refresh goes straight to Data — never back through Loading.
                assertThat(next).isInstanceOf(TimelineRevampUiState.Data::class.java)
                assertThat((next as TimelineRevampUiState.Data).sections).isEqualTo(refreshed)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that a media change is held until scrolling stops`() = runTest {
        val initial = listOf(section(groupId = "May 2026", count = 3))
        val refreshed = listOf(
            section(groupId = "Jun 2026", count = 2),
            section(groupId = "May 2026", count = 3),
        )
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(initial, refreshed)
        val updates = Channel<Unit>(capacity = 1)
        initUnderTest(mediaChanges = updates.consumeAsFlow())

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            assertThat(awaitItem().sections).isEqualTo(initial)

            underTest.onScrollingChanged(true)
            updates.send(Unit)
            advanceUntilIdle()
            // Buffered while scrolling: no re-layout yet.
            expectNoEvents()

            underTest.onScrollingChanged(false)
            advanceUntilIdle()
            assertThat(awaitItem().sections).isEqualTo(refreshed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a media change preserves the loaded nodes of unchanged sections`() = runTest {
        val may = section(groupId = "May 2026", count = 3)
        val jun = section(groupId = "Jun 2026", count = 2)
        // Initially only May; the change prepends Jun (newest-first), shifting May's global offset.
        whenever(getMediaTimelineSectionsUseCase(any(), any()))
            .thenReturn(listOf(may), listOf(jun, may))
        val mayNodes = (0 until 3).map { mock<TypedFileNode>() }
        val mayItems = (0 until 3).map { item(id = 100L + it) }
        mayNodes.forEachIndexed { i, node ->
            whenever(mediaTimelineNodeUiItemMapper(node)).thenReturn(mayItems[i])
        }
        whenever(listMediaNodesByOffsetUseCase(any(), eq(may), any(), any(), any()))
            .thenReturn(mayNodes)
        whenever(listMediaNodesByOffsetUseCase(any(), eq(jun), any(), any(), any()))
            .thenReturn(emptyList())
        val updates = Channel<Unit>(capacity = 1)
        initUnderTest(mediaChanges = updates.consumeAsFlow())

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // initial Data (May), no loaded nodes
            underTest.onVisibleRangeChanged(firstIndex = 0, lastIndex = 2)
            advanceUntilIdle()
            var data = awaitItem()
            while (data.loadedNodes.isEmpty()) data = awaitItem()
            assertThat(data.loadedNodes.values).containsExactlyElementsIn(mayItems)

            updates.send(Unit)
            advanceUntilIdle()

            // May's thumbnails survive the refresh, re-projected onto their new global indices
            // (Jun now occupies 0..1) instead of flashing back to shimmer.
            var refreshed = awaitItem()
            while (!refreshed.loadedNodes.keys.containsAll(listOf(2, 3, 4))) {
                refreshed = awaitItem()
            }
            assertThat(refreshed.sections).isEqualTo(listOf(jun, may))
            assertThat(refreshed.loadedNodes.values).containsAtLeastElementsIn(mayItems)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a content change patches the cached item in place`() = runTest {
        val may = section(groupId = "May 2026", count = 3)
        whenever(getMediaTimelineSectionsUseCase(any(), any())).thenReturn(listOf(may))
        val nodes = (0 until 3).map { mock<TypedFileNode>() }
        val items = (0 until 3).map { item(id = 100L + it) }
        nodes.forEachIndexed { i, node ->
            whenever(mediaTimelineNodeUiItemMapper(node)).thenReturn(items[i])
        }
        whenever(listMediaNodesByOffsetUseCase(any(), eq(may), any(), any(), any()))
            .thenReturn(nodes)

        // A favourite change on id 101 re-fetches that node and re-maps it to a favourited item.
        val refreshedNode = mock<TypedFileNode> { on { id } doReturn NodeId(101L) }
        val patchedItem = item(id = 101L, isFavourite = true)
        whenever(getNodeListByIdsUseCase(listOf(NodeId(101L)))).thenReturn(listOf(refreshedNode))
        whenever(mediaTimelineNodeUiItemMapper(refreshedNode)).thenReturn(patchedItem)

        val contentChanges = Channel<Set<Long>>(capacity = 1)
        initUnderTest(contentChanges = contentChanges.consumeAsFlow())

        underTest.uiState.filterIsInstance<TimelineRevampUiState.Data>().test {
            awaitItem() // initial Data, no loaded nodes
            underTest.onVisibleRangeChanged(firstIndex = 0, lastIndex = 2)
            advanceUntilIdle()
            var data = awaitItem()
            while (data.loadedNodes.isEmpty()) data = awaitItem()
            assertThat(data.loadedNodes.values).containsExactlyElementsIn(items)

            contentChanges.send(setOf(101L))
            advanceUntilIdle()

            var patched = awaitItem()
            while (!patched.loadedNodes.values.contains(patchedItem)) patched = awaitItem()
            // Only the changed slot (global index 1) is replaced; the rest are untouched.
            assertThat(patched.loadedNodes[1]).isEqualTo(patchedItem)
            assertThat(patched.loadedNodes.values).doesNotContain(items[1])
            assertThat(patched.loadedNodes[0]).isEqualTo(items[0])
            assertThat(patched.loadedNodes[2]).isEqualTo(items[2])
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun section(groupId: String, count: Long) =
        MediaTimelineSection(groupId = groupId, startDate = 0L, endDate = 0L, count = count)

    private fun item(id: Long, isFavourite: Boolean = false) = PhotosNodeContentItemV2(
        key = id,
        contentType = PhotosNodeContentType.PhotoNode,
        id = id,
        mediaType = MediaType.Image,
        day = 0,
        month = 0,
        year = 0,
        fullModificationTime = 0L,
        thumbnailFilePath = null,
        previewFilePath = null,
        extension = "jpg",
        isFavourite = isFavourite,
        isSensitive = false,
    )

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()

        const val PRIMARY_HANDLE = 100L
        const val SECONDARY_HANDLE = 200L

        val DEFAULT_FILTER = MediaTimelineFilter(
            granularity = MediaTimelineFilter.Granularity.Day,
            category = MediaTimelineFilter.Category.All,
            location = MediaTimelineFilter.Location.CloudDriveAndVault,
            sensitivity = MediaTimelineFilter.Sensitivity.ShowAll,
        )
    }
}
