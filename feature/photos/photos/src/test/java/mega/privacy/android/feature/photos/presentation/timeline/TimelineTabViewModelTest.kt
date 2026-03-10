package mega.privacy.android.feature.photos.presentation.timeline

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePhotosResult
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.entity.photos.TimelineSortedPhotosResult
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.photos.GetTimelineFilterPreferencesUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.domain.usecase.photos.SetTimelineFilterPreferencesUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.mapper.TimelineFilterUiStateMapper
import mega.privacy.android.feature.photos.model.FilterMediaSource
import mega.privacy.android.feature.photos.model.FilterMediaSource.Companion.toLocationValue
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.FilterMediaType.Companion.toMediaTypeValue
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotoToTypedNodeMapper
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotosNodeListCardMapper
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
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
import java.time.LocalDateTime
import kotlin.random.Random

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineTabViewModelTest {

    private lateinit var underTest: TimelineTabViewModel

    private val monitorTimelinePhotosUseCase: MonitorTimelinePhotosUseCase = mock()
    private val photoUiStateMapper: PhotoUiStateMapper = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val photosNodeListCardMapper: PhotosNodeListCardMapper = mock()
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase = mock()
    private val setTimelineFilterPreferencesUseCase: SetTimelineFilterPreferencesUseCase = mock()
    private val timelineFilterUiStateMapper: TimelineFilterUiStateMapper = mock()
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase = mock()
    private val photoToTypedNodeMapper = PhotoToTypedNodeMapper()
    private val analyticsTracker: AnalyticsTracker = mock()

    private val isHiddenNodesEnabledFlow = MutableStateFlow(false)

    @BeforeEach
    fun setup() = runTest {
        Analytics.initialise(analyticsTracker)
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn isHiddenNodesEnabledFlow
        underTest = TimelineTabViewModel(
            monitorTimelinePhotosUseCase = monitorTimelinePhotosUseCase,
            photoUiStateMapper = photoUiStateMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            photosNodeListCardMapper = photosNodeListCardMapper,
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase = setTimelineFilterPreferencesUseCase,
            timelineFilterUiStateMapper = timelineFilterUiStateMapper,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            photoToTypedNodeMapper = photoToTypedNodeMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Analytics.initialise(null)
        reset(
            monitorTimelinePhotosUseCase,
            photoUiStateMapper,
            fileTypeIconMapper,
            photosNodeListCardMapper,
            getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase,
            timelineFilterUiStateMapper,
            monitorHiddenNodesEnabledUseCase,
            analyticsTracker
        )
    }

    @Test
    fun `test that photos are fetched successfully`() = runTest {
        val now = LocalDateTime.now()
        val mockFileTypeInfo = mock<VideoFileTypeInfo>()
        val photo1 = mock<Photo.Image> {
            on { id } doReturn 1L
            on { modificationTime } doReturn now
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult1 = PhotoResult(
            photo = photo1,
            isMarkedSensitive = false
        )
        val photoUiState1 = mock<PhotoUiState.Image>()
        whenever(
            photoUiStateMapper.invoke(photo = photo1)
        ) doReturn photoUiState1
        val photo2 = mock<Photo.Video> {
            on { id } doReturn 2L
            on { modificationTime } doReturn now.minusDays(1)
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult2 = PhotoResult(
            photo = photo2,
            isMarkedSensitive = true
        )
        val photosResult = TimelinePhotosResult(
            allPhotos = listOf(photoResult1, photoResult2),
            nonSensitivePhotos = listOf(photoResult1)
        )
        val sortResult = mock<TimelineSortedPhotosResult> {
            on { sortedPhotos } doReturn emptyList()
        }
        whenever(
            monitorTimelinePhotosUseCase.sortPhotos(
                photos = photosResult.nonSensitivePhotos,
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn sortResult
        val photoUiState2 = mock<PhotoUiState.Video>()
        whenever(
            photoUiStateMapper.invoke(photo = photo2)
        ) doReturn photoUiState2
        whenever(
            monitorTimelinePhotosUseCase.invoke(request = any())
        ) doReturn flowOf(photosResult)
        whenever(
            photosNodeListCardMapper.invoke(photosDateResults = any())
        ) doReturn persistentListOf()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().allPhotos.size).isEqualTo(2)
        }
        verify(monitorTimelinePhotosUseCase).invoke(isA<TimelinePhotosRequest>())
        verify(monitorTimelinePhotosUseCase).sortPhotos(
            photos = photosResult.nonSensitivePhotos,
            sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        )
    }

    @Test
    fun `test that all photos are resorted when the sort order changes`() = runTest {
        val photosResult = mock<TimelinePhotosResult> {
            on { allPhotos } doReturn emptyList()
            on { nonSensitivePhotos } doReturn emptyList()
        }
        whenever(monitorTimelinePhotosUseCase(request = any())) doReturn flowOf(photosResult)
        val sortResult = mock<TimelineSortedPhotosResult> {
            on { sortedPhotos } doReturn emptyList()
        }
        whenever(
            monitorTimelinePhotosUseCase.sortPhotos(
                photos = eq(emptyList()),
                sortOrder = any()
            )
        ) doReturn sortResult
        whenever(
            photosNodeListCardMapper.invoke(photosDateResults = any())
        ) doReturn persistentListOf()

        underTest.onSortOptionsChange(value = TimelineTabSortOptions.Oldest)

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().currentSort
            ).isEqualTo(TimelineTabSortOptions.Oldest)
        }
        verify(monitorTimelinePhotosUseCase).sortPhotos(
            photos = any(),
            sortOrder = eq(SortOrder.ORDER_MODIFICATION_ASC)
        )
    }

    @ParameterizedTest
    @EnumSource(TimelineGridSize::class)
    fun `test that the correct grid size is set`(gridSize: TimelineGridSize) =
        runTest {
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn emptyList()
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(monitorTimelinePhotosUseCase(request = any())) doReturn flowOf(photosResult)
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()

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
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn emptyList()
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photo1 = mock<Photo.Image> {
                on { id } doReturn 1L
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2 = mock<Photo.Image> {
                on { id } doReturn 2L
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo2)
            ) doReturn photo2UiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult1, photoResult2)
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()

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
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn emptyList()
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photo1 = mock<Photo.Image> {
                on { id } doReturn 1L
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2 = mock<Photo.Image> {
                on { id } doReturn 2L
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo2)
            ) doReturn photo2UiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult1, photoResult2)
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()

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
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn emptyList()
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photo1 = mock<Photo.Image> {
                on { id } doReturn 1L
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2 = mock<Photo.Image> {
                on { id } doReturn 2L
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo2)
            ) doReturn photo2UiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult1, photoResult2)
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()

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
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn emptyList()
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photo1 = mock<Photo.Image> {
                on { id } doReturn 1L
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2 = mock<Photo.Image> {
                on { id } doReturn 2L
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo2)
            ) doReturn photo2UiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult1, photoResult2)
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()

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
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn emptyList()
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(monitorTimelinePhotosUseCase(request = any())) doReturn flowOf(photosResult)
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()

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
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn emptyList()
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photo1 = mock<Photo.Image> {
                on { id } doReturn 1L
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2 = mock<Photo.Image> {
                on { id } doReturn 2L
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image>()
            whenever(
                photoUiStateMapper.invoke(photo = photo2)
            ) doReturn photo2UiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult1, photoResult2)
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()

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

        assertThat(actual).isEqualTo(PhotoModificationTimePeriod.All)
    }

    @ParameterizedTest
    @EnumSource(PhotoModificationTimePeriod::class)
    fun `test that the selected time period is successfully updated`(timePeriod: PhotoModificationTimePeriod) =
        runTest {
            underTest.onPhotoTimePeriodSelected(value = timePeriod)

            val actual = underTest.selectedTimePeriod

            assertThat(actual).isEqualTo(timePeriod)
        }

    @Test
    fun `test that the action state is ready when the displayed photos are set`() = runTest {
        val photosResult = mock<TimelinePhotosResult> {
            on { allPhotos } doReturn emptyList()
            on { nonSensitivePhotos } doReturn emptyList()
        }
        whenever(
            monitorTimelinePhotosUseCase.invoke(request = any())
        ) doReturn flowOf(photosResult)
        val now = LocalDateTime.now()
        val mockFileTypeInfo = mock<VideoFileTypeInfo>()
        val photo1 = mock<Photo.Image> {
            on { id } doReturn 1L
            on { modificationTime } doReturn now
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult1 = PhotoResult(
            photo = photo1,
            isMarkedSensitive = false
        )
        val photo1UiState = mock<PhotoUiState.Image>()
        whenever(
            photoUiStateMapper.invoke(photo = photo1)
        ) doReturn photo1UiState
        val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
        val photo2 = mock<Photo.Image> {
            on { id } doReturn 2L
            on { modificationTime } doReturn now.plusMonths(2)
            on { fileTypeInfo } doReturn mockTextFileTypeInfo
        }
        val photoResult2 = PhotoResult(
            photo = photo2,
            isMarkedSensitive = false
        )
        val photo2UiState = mock<PhotoUiState.Image>()
        whenever(
            photoUiStateMapper.invoke(photo = photo2)
        ) doReturn photo2UiState
        val sortResult = mock<TimelineSortedPhotosResult> {
            on { sortedPhotos } doReturn listOf(photoResult1, photoResult2)
        }
        whenever(
            monitorTimelinePhotosUseCase.sortPhotos(
                photos = eq(emptyList()),
                sortOrder = any()
            )
        ) doReturn sortResult
        whenever(
            photosNodeListCardMapper.invoke(photosDateResults = any())
        ) doReturn persistentListOf()

        underTest.uiState.test { cancelAndConsumeRemainingEvents() }

        underTest.actionUiState.test {
            assertThat(expectMostRecentItem().isReady).isTrue()
        }
    }
}
