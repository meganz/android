package mega.privacy.android.feature.photos.presentation.timeline

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.formatter.mapper.DurationInSecondsTextMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.photos.FilterMediaType
import mega.privacy.android.domain.entity.photos.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelineMediaUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.model.MediaTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCardPeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlin.time.Duration

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineTabViewModelTest {

    private lateinit var underTest: TimelineTabViewModel

    private val monitorTimelineMediaUseCase: MonitorTimelineMediaUseCase = mock()
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase = mock()
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase = mock()
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val durationInSecondsTextMapper: DurationInSecondsTextMapper = mock()
    private val analyticsTracker: AnalyticsTracker = mock()

    private val isHiddenNodesEnabledFlow = MutableStateFlow(false)

    @BeforeEach
    fun setup() = runTest {
        Analytics.initialise(analyticsTracker)
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn isHiddenNodesEnabledFlow
        underTest = TimelineTabViewModel(
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase = setTimelineFilterPreferencesUseCase,
            timelineFilterUiStateMapper = timelineFilterUiStateMapper,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            monitorTimelineMediaUseCase = monitorTimelineMediaUseCase,
            durationInSecondsTextMapper = durationInSecondsTextMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Analytics.initialise(null)
        reset(
            monitorTimelineMediaUseCase,
            getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase,
            timelineFilterUiStateMapper,
            monitorHiddenNodesEnabledUseCase,
            durationInSecondsTextMapper,
            analyticsTracker
        )
    }

    @Test
    fun `test that photos are fetched successfully`() = runTest {
        val now = ZonedDateTime.now()
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val photo1 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 1L)
            on { modificationTime } doReturn now.toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn true
        }
        val videoDuration = Duration.ZERO
        val mockVideoFileTypeInfo = mock<VideoFileTypeInfo> {
            on { extension } doReturn ".mp4"
            on { duration } doReturn videoDuration
        }
        val photo2 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 2L)
            on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
            on { type } doReturn mockVideoFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        whenever(
            durationInSecondsTextMapper(
                duration = videoDuration
            )
        ) doReturn "0:16"
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = listOf(photo1, photo2),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn listOf(photo2, photo1)
        whenever(
            monitorTimelineMediaUseCase.invoke(request = any())
        ) doReturn flowOf(listOf(photo1, photo2))

        underTest.uiState.test {
            assertThat(expectMostRecentItem().displayedPhotos.size).isEqualTo(3) // 2 Photos 1 Header
        }
        verify(monitorTimelineMediaUseCase).invoke(isA<TimelinePhotosRequest>())
        verify(monitorTimelineMediaUseCase).sortMedia(
            nodes = listOf(photo1, photo2),
            sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        )
    }

    @Test
    fun `test that all photos are resorted when the sort order changes`() = runTest {
        whenever(
            monitorTimelineMediaUseCase.invoke(request = any())
        ) doReturn flowOf(emptyList())
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = eq(emptyList()),
                sortOrder = any()
            )
        ) doReturn emptyList()

        underTest.onSortOptionsChange(value = TimelineTabSortOptions.Oldest)

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().currentSort
            ).isEqualTo(TimelineTabSortOptions.Oldest)
        }
        verify(monitorTimelineMediaUseCase).sortMedia(
            nodes = any(),
            sortOrder = eq(SortOrder.ORDER_MODIFICATION_ASC)
        )
    }

    @ParameterizedTest
    @EnumSource(TimelineGridSize::class)
    fun `test that the correct grid size is set`(gridSize: TimelineGridSize) =
        runTest {
            whenever(
                monitorTimelineMediaUseCase.invoke(request = any())
            ) doReturn flowOf(emptyList())
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn emptyList()

            underTest.onGridSizeChange(
                size = gridSize,
                isEnableCameraUploadPageShowing = true,
                mediaSource = FilterMediaSource.CloudDrive
            )

            underTest.uiState.test {
                assertThat(expectMostRecentItem().gridSize).isEqualTo(gridSize)
            }
        }

    @Test
    fun `test that the sort toolbar action is disabled when grid size is changed and the camera upload page is displayed and the media source is not cloud drive`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn now.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn true
            }
            val videoDuration = Duration.ZERO
            val mockVideoFileTypeInfo = mock<VideoFileTypeInfo> {
                on { extension } doReturn ".mp4"
                on { duration } doReturn videoDuration
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 2L)
                on { modificationTime } doReturn now.plusMonths(2).toEpochSecond()
                on { type } doReturn mockVideoFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(
                durationInSecondsTextMapper(
                    duration = videoDuration
                )
            ) doReturn "0:16"
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo2, photo1)
            whenever(
                monitorTimelineMediaUseCase.invoke(request = any())
            ) doReturn flowOf(listOf(photo1, photo2))

            underTest.uiState.test { cancelAndConsumeRemainingEvents() }
            underTest.onGridSizeChange(
                size = TimelineGridSize.Large,
                isEnableCameraUploadPageShowing = true,
                mediaSource = FilterMediaSource.CameraUpload
            )

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().normalModeItem.enableSort).isFalse()
            }
        }

    @Test
    fun `test that the sort toolbar action is disabled when grid size is changed and no photos to display`() =
        runTest {
            underTest.onGridSizeChange(
                size = TimelineGridSize.Large,
                isEnableCameraUploadPageShowing = false,
                mediaSource = FilterMediaSource.CloudDrive
            )

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().normalModeItem.enableSort).isFalse()
            }
        }

    @Test
    fun `test that the sort toolbar action is enabled when grid size is changed and the camera upload page is not displayed and the media source is cloud drive`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn now.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn true
            }
            val videoDuration = Duration.ZERO
            val mockVideoFileTypeInfo = mock<VideoFileTypeInfo> {
                on { extension } doReturn ".mp4"
                on { duration } doReturn videoDuration
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 2L)
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { type } doReturn mockVideoFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(
                durationInSecondsTextMapper(
                    duration = videoDuration
                )
            ) doReturn "0:16"
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo2, photo1)
            whenever(
                monitorTimelineMediaUseCase.invoke(request = any())
            ) doReturn flowOf(listOf(photo1, photo2))

            underTest.uiState.test { cancelAndConsumeRemainingEvents() }
            underTest.onGridSizeChange(
                size = TimelineGridSize.Large,
                isEnableCameraUploadPageShowing = false,
                mediaSource = FilterMediaSource.CloudDrive
            )

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().normalModeItem.enableSort).isTrue()
            }
        }

    @Test
    fun `test that the sort toolbar action is disabled when CU page is enabled and the media source is not cloud drive`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn now.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn true
            }
            val videoDuration = Duration.ZERO
            val mockVideoFileTypeInfo = mock<VideoFileTypeInfo> {
                on { extension } doReturn ".mp4"
                on { duration } doReturn videoDuration
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 2L)
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { type } doReturn mockVideoFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(
                durationInSecondsTextMapper(
                    duration = videoDuration
                )
            ) doReturn "0:16"
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo2, photo1)
            whenever(
                monitorTimelineMediaUseCase.invoke(request = any())
            ) doReturn flowOf(listOf(photo1, photo2))

            underTest.updateSortActionBasedOnCUPageEnablement(
                isEnableCameraUploadPageShowing = true,
                mediaSource = FilterMediaSource.CameraUpload,
                isCUPageEnabled = true
            )

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().normalModeItem.enableSort).isFalse()
            }
        }

    @Test
    fun `test that the sort toolbar action is disabled when CU page is disabled and the media source is cloud drive`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn now.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn true
            }
            val videoDuration = Duration.ZERO
            val mockVideoFileTypeInfo = mock<VideoFileTypeInfo> {
                on { extension } doReturn ".mp4"
                on { duration } doReturn videoDuration
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 2L)
                on { modificationTime } doReturn now.minusDays(1).toEpochSecond()
                on { type } doReturn mockVideoFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(
                durationInSecondsTextMapper(
                    duration = videoDuration
                )
            ) doReturn "0:16"
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo2, photo1)
            whenever(
                monitorTimelineMediaUseCase.invoke(request = any())
            ) doReturn flowOf(listOf(photo1, photo2))

            underTest.updateSortActionBasedOnCUPageEnablement(
                isEnableCameraUploadPageShowing = true,
                mediaSource = FilterMediaSource.CloudDrive,
                isCUPageEnabled = false
            )

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().normalModeItem.enableSort).isFalse()
            }
        }

    @Test
    fun `test that the sort toolbar action is disabled when CU page is disabled and no photos to display`() =
        runTest {
            whenever(
                monitorTimelineMediaUseCase.invoke(request = any())
            ) doReturn flowOf(emptyList())
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn emptyList()

            underTest.updateSortActionBasedOnCUPageEnablement(
                isEnableCameraUploadPageShowing = false,
                mediaSource = FilterMediaSource.CloudDrive,
                isCUPageEnabled = false
            )

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().normalModeItem.enableSort).isFalse()
            }
        }

    @Test
    fun `test that the sort toolbar action is enabled when CU page is disabled and the camera upload page is not displayed and the media source is cloud drive`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn now.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn true
            }
            val videoDuration = Duration.ZERO
            val mockVideoFileTypeInfo = mock<VideoFileTypeInfo> {
                on { extension } doReturn ".mp4"
                on { duration } doReturn videoDuration
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 2L)
                on { modificationTime } doReturn now.plusMonths(2).toEpochSecond()
                on { type } doReturn mockVideoFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(
                durationInSecondsTextMapper(
                    duration = videoDuration
                )
            ) doReturn "0:16"
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo2, photo1)
            whenever(
                monitorTimelineMediaUseCase.invoke(request = any())
            ) doReturn flowOf(listOf(photo1, photo2))

            underTest.uiState.test { cancelAndConsumeRemainingEvents() }
            underTest.updateSortActionBasedOnCUPageEnablement(
                isEnableCameraUploadPageShowing = false,
                mediaSource = FilterMediaSource.CloudDrive,
                isCUPageEnabled = false
            )

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().normalModeItem.enableSort).isTrue()
            }
        }

    @Test
    fun `test that the default filter preference map values are used to set the filter when no new filter is applied`() =
        runTest {
            val preferenceMap = mapOf<String, String?>()
            whenever(getTimelineFilterPreferencesUseCase()) doReturn preferenceMap

            underTest.filterUiState.test { cancelAndConsumeRemainingEvents() }
            verify(timelineFilterUiStateMapper).invoke(
                preferenceMap = preferenceMap,
                shouldApplyFilterFromPreference = false
            )
        }

    @Test
    fun `test that the new filter values are used to set the filter when a new filter is applied`() =
        runTest {
            val preferenceMap = mapOf<String, String?>()
            val isRemembered = Random.nextBoolean()
            val mediaType = FilterMediaType.entries.random()
            val mediaSource = FilterMediaSource.entries.random()
            val newFilter = mapOf(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to isRemembered.toString(),
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to mediaType.toMediaTypeValue(),
                TimelinePreferencesJSON.JSON_KEY_LOCATION.value to mediaSource.toLocationValue(),
            )
            whenever(getTimelineFilterPreferencesUseCase()) doReturn preferenceMap

            underTest.onFilterChange(
                TimelineFilterRequest(
                    isRemembered = isRemembered,
                    mediaType = mediaType,
                    mediaSource = mediaSource
                )
            )

            underTest.filterUiState.test { cancelAndConsumeRemainingEvents() }
            verify(timelineFilterUiStateMapper).invoke(
                preferenceMap = newFilter,
                shouldApplyFilterFromPreference = true
            )
            verify(setTimelineFilterPreferencesUseCase).invoke(newFilter)
        }

    @Test
    fun `test that time period All is selected by default`() = runTest {
        val actual = underTest.selectedTimePeriod

        assertThat(actual).isEqualTo(MediaTimePeriod.All)
    }

    @ParameterizedTest
    @EnumSource(MediaTimePeriod::class)
    fun `test that the selected time period is successfully updated`(timePeriod: MediaTimePeriod) =
        runTest {
            underTest.onMediaTimePeriodSelected(value = timePeriod)

            val actual = underTest.selectedTimePeriod

            assertThat(actual).isEqualTo(timePeriod)
        }

    @Test
    fun `test that the action state is ready when the displayed photos are set`() = runTest {
        val now = ZonedDateTime.now()
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val photo1 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 1L)
            on { modificationTime } doReturn now.toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn true
        }
        val videoDuration = Duration.ZERO
        val mockVideoFileTypeInfo = mock<VideoFileTypeInfo> {
            on { extension } doReturn ".mp4"
            on { duration } doReturn videoDuration
        }
        val photo2 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 2L)
            on { modificationTime } doReturn now.plusMonths(2).toEpochSecond()
            on { type } doReturn mockVideoFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        whenever(
            durationInSecondsTextMapper(
                duration = videoDuration
            )
        ) doReturn "0:16"
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = listOf(photo1, photo2),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn listOf(photo2, photo1)
        whenever(
            monitorTimelineMediaUseCase.invoke(request = any())
        ) doReturn flowOf(listOf(photo1, photo2))

        underTest.uiState.test { cancelAndConsumeRemainingEvents() }

        underTest.actionUiState.test {
            assertThat(expectMostRecentItem().isReady).isTrue()
        }
    }

    @Test
    fun `test that photosInDay contains one entry per distinct calendar day`() = runTest {
        val dateTime = ZonedDateTime.of(
            2026, 4, 25,
            10, 30, 0,
            0,
            ZoneId.of("Asia/Jakarta")
        )
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val photo1 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 1L)
            on { modificationTime } doReturn dateTime.toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        val photo2 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 2L)
            on { modificationTime } doReturn dateTime.plusHours(1).toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        val photo3 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 3L)
            on { modificationTime } doReturn dateTime.minusDays(1).toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                flowOf(listOf(photo1, photo2, photo3))
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = listOf(photo1, photo2, photo3),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn listOf(photo1, photo2, photo3)

        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.daysCardPhotos.size).isEqualTo(2)
            assertThat(item.daysCardPhotos.all { it.period == PhotosNodeListCardPeriod.Day }).isTrue()
        }
    }

    @Test
    fun `test that photosInDay count reflects the number of photos grouped under each day`() =
        runTest {
            val dateTime = ZonedDateTime.of(
                2026, 4, 25,
                10, 30, 0,
                0,
                ZoneId.of("Asia/Jakarta")
            )
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn dateTime.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 2L)
                on { modificationTime } doReturn dateTime.plusHours(1).toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            val photo3 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 3L)
                on { modificationTime } doReturn dateTime.plusHours(2).toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                    flowOf(listOf(photo1, photo2, photo3))
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2, photo3),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo1, photo2, photo3)

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.daysCardPhotos.size).isEqualTo(1)
                assertThat(item.daysCardPhotos.first().count).isEqualTo(3)
            }
        }

    @Test
    fun `test that photosInDay uses the first photo of each day as the card representative`() =
        runTest {
            val dateTime = ZonedDateTime.of(
                2026, 4, 25,
                10, 30, 0,
                0,
                ZoneId.of("Asia/Jakarta")
            )
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn dateTime.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 2L)
                on { modificationTime } doReturn dateTime.plusHours(2).toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                    flowOf(listOf(photo1, photo2))
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo1, photo2)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().daysCardPhotos.first().id).isEqualTo(1L)
            }
        }

    @Test
    fun `test that photosInMonth contains one entry per distinct year-month`() = runTest {
        val now = ZonedDateTime.now()
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val photo1 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 1L)
            on { modificationTime } doReturn now.toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        val photo2 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 2L)
            on { modificationTime } doReturn now.plusDays(5).toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        val photo3 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 3L)
            on { modificationTime } doReturn now.minusMonths(1).toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                flowOf(listOf(photo1, photo2, photo3))
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = listOf(photo1, photo2, photo3),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn listOf(photo1, photo2, photo3)

        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.monthsCardPhotos.size).isEqualTo(2)
            assertThat(item.monthsCardPhotos.all { it.period == PhotosNodeListCardPeriod.Month }).isTrue()
        }
    }

    @Test
    fun `test that photosInMonth groups photos from the same month across different days into one card`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photos = (1..4).map { i ->
                mock<TypedFileNode> {
                    on { id } doReturn NodeId(longValue = i.toLong())
                    on { modificationTime } doReturn now.plusDays(i.toLong()).toEpochSecond()
                    on { type } doReturn mockImageFileTypeInfo
                    on { isMarkedSensitive } doReturn false
                    on { isSensitiveInherited } doReturn false
                    on { isFavourite } doReturn false
                }
            }
            whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                    flowOf(photos)
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = photos,
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn photos

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.monthsCardPhotos.size).isEqualTo(1)
                assertThat(item.monthsCardPhotos.first().count).isEqualTo(4)
            }
        }

    @Test
    fun `test that photosInMonth formattedDate shows month only when photo year matches current year`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn now.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn flowOf(
                listOf(
                    photo
                )
            )
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo)

            val expectedDate = TimelineFormatters.month.format(now)

            underTest.uiState.test {
                assertThat(expectMostRecentItem().monthsCardPhotos.first().formattedDate).isEqualTo(
                    expectedDate
                )
            }
        }

    @Test
    fun `test that photosInMonth formattedDate shows month and year when photo year differs from current year`() =
        runTest {
            val lastYear = ZonedDateTime.now().minusYears(1)
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 1L)
                on { modificationTime } doReturn lastYear.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn flowOf(
                listOf(
                    photo
                )
            )
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo)

            underTest.uiState.test {
                val expected = TimelineFormatters.monthYear.format(lastYear)
                assertThat(expectMostRecentItem().monthsCardPhotos.first().formattedDate).isEqualTo(
                    expected
                )
            }
        }

    @Test
    fun `test that photosInYear contains one entry per distinct year`() = runTest {
        val now = ZonedDateTime.now()
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val photo1 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 1L)
            on { modificationTime } doReturn now.toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        val photo2 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 2L)
            on { modificationTime } doReturn now.minusMonths(3).toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        val photo3 = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 3L)
            on { modificationTime } doReturn now.minusYears(1).toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                flowOf(listOf(photo1, photo2, photo3))
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = listOf(photo1, photo2, photo3),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn listOf(photo1, photo2, photo3)

        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.yearsCardPhotos.size).isEqualTo(2)
            assertThat(item.yearsCardPhotos.all { it.period == PhotosNodeListCardPeriod.Year }).isTrue()
        }
    }

    @Test
    fun `test that photosInYear groups all photos from the same year into one card`() = runTest {
        val now = ZonedDateTime.now().withMonth(7)
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val photos = (1..6).map { i ->
            mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = i.toLong())
                on { modificationTime } doReturn now.minusMonths(i.toLong()).toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
        }
        whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn flowOf(photos)
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = photos,
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn photos

        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.yearsCardPhotos.size).isEqualTo(1)
            assertThat(item.yearsCardPhotos.first().count).isEqualTo(6)
        }
    }

    @Test
    fun `test that photosInYear formattedDate shows the year correctly`() = runTest {
        val lastYear = ZonedDateTime.now().minusYears(1)
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val photo = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 1L)
            on { modificationTime } doReturn lastYear.toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn flowOf(
            listOf(
                photo
            )
        )
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = listOf(photo),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn listOf(photo)

        underTest.uiState.test {
            val expected = TimelineFormatters.year.format(lastYear)
            assertThat(
                expectMostRecentItem().yearsCardPhotos.first().formattedDate
            ).isEqualTo(expected)
        }
    }

    @Test
    fun `test that photosInDay photosInMonth and photosInYear are all empty when there are no photos`() =
        runTest {
            whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn flowOf(
                emptyList()
            )
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn emptyList()

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.daysCardPhotos).isEmpty()
                assertThat(item.monthsCardPhotos).isEmpty()
                assertThat(item.yearsCardPhotos).isEmpty()
            }
        }

    @Test
    fun `test that photosInDay photosInMonth and photosInYear card keys match the node id of the first photo in each group`() =
        runTest {
            val now = ZonedDateTime.now()
            val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
                on { extension } doReturn ".jpg"
            }
            val photo1 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 10L)
                on { modificationTime } doReturn now.toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            val photo2 = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = 20L)
                on { modificationTime } doReturn now.minusYears(1).toEpochSecond()
                on { type } doReturn mockImageFileTypeInfo
                on { isMarkedSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
                on { isFavourite } doReturn false
            }
            whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                    flowOf(listOf(photo1, photo2))
            whenever(
                monitorTimelineMediaUseCase.sortMedia(
                    nodes = listOf(photo1, photo2),
                    sortOrder = SortOrder.ORDER_MODIFICATION_DESC
                )
            ) doReturn listOf(photo1, photo2)

            underTest.uiState.test {
                val item = expectMostRecentItem()
                assertThat(item.daysCardPhotos.map { it.key }).containsExactly(10L, 20L).inOrder()
                assertThat(item.monthsCardPhotos.map { it.key }).containsExactly(10L, 20L)
                    .inOrder()
                assertThat(item.yearsCardPhotos.map { it.key }).containsExactly(10L, 20L).inOrder()
            }
        }

    @Test
    fun `test that sensitive photos are correctly flagged in day month and year cards`() = runTest {
        val now = ZonedDateTime.now()
        val mockImageFileTypeInfo = mock<GifFileTypeInfo> {
            on { extension } doReturn ".jpg"
        }
        val sensitivePhoto = mock<TypedFileNode> {
            on { id } doReturn NodeId(longValue = 1L)
            on { modificationTime } doReturn now.toEpochSecond()
            on { type } doReturn mockImageFileTypeInfo
            on { isMarkedSensitive } doReturn true
            on { isSensitiveInherited } doReturn false
            on { isFavourite } doReturn false
        }
        whenever(monitorTimelineMediaUseCase.invoke(request = any())) doReturn
                flowOf(listOf(sensitivePhoto))
        whenever(
            monitorTimelineMediaUseCase.sortMedia(
                nodes = listOf(sensitivePhoto),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn listOf(sensitivePhoto)

        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.daysCardPhotos.first().isSensitive).isTrue()
            assertThat(item.monthsCardPhotos.first().isSensitive).isTrue()
            assertThat(item.yearsCardPhotos.first().isSensitive).isTrue()
        }
    }
}
