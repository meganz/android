package mega.privacy.android.feature.photos.presentation.timeline

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.ExportedData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePhotosResult
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.entity.photos.TimelineSortedPhotosResult
import mega.privacy.android.domain.usecase.GetNodeListByIdsUseCase
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
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentType
import mega.privacy.android.feature.photos.model.TimelineGridSize
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotosNodeListCardMapper
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoModificationTimePeriod
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineFilterRequest
import mega.privacy.android.feature.photos.presentation.timeline.model.TimelineSelectionMenuAction
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
    private val getNodeListByIdsUseCase: GetNodeListByIdsUseCase = mock()

    private val isHiddenNodesEnabledFlow = MutableStateFlow(false)

    @BeforeEach
    fun setup() = runTest {
        whenever(monitorHiddenNodesEnabledUseCase()) doReturn isHiddenNodesEnabledFlow
        whenever(getNodeListByIdsUseCase(nodeIds = any())) doReturn emptyList()
        underTest = TimelineTabViewModel(
            monitorTimelinePhotosUseCase = monitorTimelinePhotosUseCase,
            photoUiStateMapper = photoUiStateMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            photosNodeListCardMapper = photosNodeListCardMapper,
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase = setTimelineFilterPreferencesUseCase,
            timelineFilterUiStateMapper = timelineFilterUiStateMapper,
            monitorHiddenNodesEnabledUseCase = monitorHiddenNodesEnabledUseCase,
            getNodeListByIdsUseCase = getNodeListByIdsUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorTimelinePhotosUseCase,
            photoUiStateMapper,
            fileTypeIconMapper,
            photosNodeListCardMapper,
            getTimelineFilterPreferencesUseCase,
            setTimelineFilterPreferencesUseCase,
            timelineFilterUiStateMapper,
            monitorHiddenNodesEnabledUseCase,
            getNodeListByIdsUseCase
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
    fun `test that the first displayed header item shows the grid size settings`() = runTest {
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

        underTest.uiState.test {
            val item = expectMostRecentItem()
            assertThat(item.displayedPhotos[0]).isEqualTo(
                PhotosNodeContentType.HeaderItem(
                    time = photoResult1.photo.modificationTime,
                    shouldShowGridSizeSettings = true
                )
            )
            assertThat(item.displayedPhotos[2]).isEqualTo(
                PhotosNodeContentType.HeaderItem(
                    time = photoResult2.photo.modificationTime,
                    shouldShowGridSizeSettings = false
                )
            )
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
    fun `test that a photo is successfully selected`() = runTest {
        val photosResult = mock<TimelinePhotosResult> {
            on { allPhotos } doReturn emptyList()
            on { nonSensitivePhotos } doReturn emptyList()
        }
        whenever(
            monitorTimelinePhotosUseCase.invoke(request = any())
        ) doReturn flowOf(photosResult)
        val now = LocalDateTime.now()
        val mockFileTypeInfo = mock<VideoFileTypeInfo>()
        val photo1Id = 1L
        val photo1 = mock<Photo.Image> {
            on { id } doReturn photo1Id
            on { modificationTime } doReturn now
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult1 = PhotoResult(
            photo = photo1,
            isMarkedSensitive = false
        )
        val photo1UiState = mock<PhotoUiState.Image> {
            on { id } doReturn photo1Id
        }
        whenever(
            photoUiStateMapper.invoke(photo = photo1)
        ) doReturn photo1UiState
        val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
        val photo2Id = 2L
        val photo2 = mock<Photo.Image> {
            on { id } doReturn photo2Id
            on { modificationTime } doReturn now.plusMonths(2)
            on { fileTypeInfo } doReturn mockTextFileTypeInfo
        }
        val photoResult2 = PhotoResult(
            photo = photo2,
            isMarkedSensitive = false
        )
        val photo2UiState = mock<PhotoUiState.Image> {
            on { id } doReturn photo2Id
        }
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

        underTest.uiState.test {
            val selectedPhoto =
                awaitItem().displayedPhotos.find { it is PhotosNodeContentType.PhotoNodeItem }
            underTest.onPhotoSelected((selectedPhoto as PhotosNodeContentType.PhotoNodeItem).node)

            val item = expectMostRecentItem()
            assertThat(item.selectedPhotoCount).isEqualTo(1)
            val selectedPhotoFinal =
                item.displayedPhotos.find { it is PhotosNodeContentType.PhotoNodeItem }
            assertThat((selectedPhotoFinal as PhotosNodeContentType.PhotoNodeItem).node.isSelected).isTrue()
        }
    }

    @Test
    fun `test that a photo is successfully unselected`() = runTest {
        val photosResult = mock<TimelinePhotosResult> {
            on { allPhotos } doReturn emptyList()
            on { nonSensitivePhotos } doReturn emptyList()
        }
        whenever(
            monitorTimelinePhotosUseCase.invoke(request = any())
        ) doReturn flowOf(photosResult)
        val now = LocalDateTime.now()
        val mockFileTypeInfo = mock<VideoFileTypeInfo>()
        val photo1Id = 1L
        val photo1 = mock<Photo.Image> {
            on { id } doReturn photo1Id
            on { modificationTime } doReturn now
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult1 = PhotoResult(
            photo = photo1,
            isMarkedSensitive = false
        )
        val photo1UiState = mock<PhotoUiState.Image> {
            on { id } doReturn photo1Id
        }
        whenever(
            photoUiStateMapper.invoke(photo = photo1)
        ) doReturn photo1UiState
        val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
        val photo2Id = 2L
        val photo2 = mock<Photo.Image> {
            on { id } doReturn photo2Id
            on { modificationTime } doReturn now.plusMonths(2)
            on { fileTypeInfo } doReturn mockTextFileTypeInfo
        }
        val photoResult2 = PhotoResult(
            photo = photo2,
            isMarkedSensitive = false
        )
        val photo2UiState = mock<PhotoUiState.Image> {
            on { id } doReturn photo2Id
        }
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

        underTest.uiState.test {
            val photo =
                awaitItem().displayedPhotos.find { it is PhotosNodeContentType.PhotoNodeItem } as PhotosNodeContentType.PhotoNodeItem
            underTest.onPhotoSelected(photo.node) // Selection
            underTest.onPhotoSelected(photo.node) // Deselection

            val item = expectMostRecentItem()
            assertThat(item.selectedPhotoCount).isEqualTo(0)
            val latestPhoto =
                item.displayedPhotos.find { it is PhotosNodeContentType.PhotoNodeItem }
            assertThat((latestPhoto as PhotosNodeContentType.PhotoNodeItem).node.isSelected).isFalse()
        }
    }

    @Test
    fun `test that all photos are successfully selected`() = runTest {
        val photosResult = mock<TimelinePhotosResult> {
            on { allPhotos } doReturn emptyList()
            on { nonSensitivePhotos } doReturn emptyList()
        }
        whenever(
            monitorTimelinePhotosUseCase.invoke(request = any())
        ) doReturn flowOf(photosResult)
        val now = LocalDateTime.now()
        val mockFileTypeInfo = mock<VideoFileTypeInfo>()
        val photo1Id = 1L
        val photo1 = mock<Photo.Image> {
            on { id } doReturn photo1Id
            on { modificationTime } doReturn now
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult1 = PhotoResult(
            photo = photo1,
            isMarkedSensitive = false
        )
        val photo1UiState = mock<PhotoUiState.Image> {
            on { id } doReturn photo1Id
        }
        whenever(
            photoUiStateMapper.invoke(photo = photo1)
        ) doReturn photo1UiState
        val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
        val photo2Id = 2L
        val photo2 = mock<Photo.Image> {
            on { id } doReturn photo2Id
            on { modificationTime } doReturn now.plusMonths(2)
            on { fileTypeInfo } doReturn mockTextFileTypeInfo
        }
        val photoResult2 = PhotoResult(
            photo = photo2,
            isMarkedSensitive = false
        )
        val photo2UiState = mock<PhotoUiState.Image> {
            on { id } doReturn photo2Id
        }
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

        underTest.uiState.test {
            underTest.onSelectAllPhotos()

            val item = expectMostRecentItem()
            assertThat(item.selectedPhotoCount).isEqualTo(2)
            val selectedPhotos = item.displayedPhotos.filter {
                it is PhotosNodeContentType.PhotoNodeItem && it.node.isSelected
            }
            assertThat(selectedPhotos.size).isEqualTo(2)
        }
    }

    @Test
    fun `test that nothing happens when selecting all photos if all photos are already selected`() =
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
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo1Id
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo2Id
            }
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

            underTest.uiState.test {
                awaitItem()
                underTest.onSelectAllPhotos()
                awaitItem()
                underTest.onSelectAllPhotos()

                expectNoEvents()
            }
        }

    @Test
    fun `test that all photos are successfully deselected`() =
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
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo1Id
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo2Id
            }
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

            underTest.uiState.test {
                awaitItem()
                underTest.onSelectAllPhotos()
                awaitItem()
                underTest.onDeselectAllPhotos()

                val item = expectMostRecentItem()
                assertThat(item.selectedPhotoCount).isEqualTo(0)
                val selectedPhotos = item.displayedPhotos.filter {
                    it is PhotosNodeContentType.PhotoNodeItem && it.node.isSelected
                }
                assertThat(selectedPhotos.size).isEqualTo(0)
            }
        }

    @Test
    fun `test that nothing happens when deselecting all photos if all photos are already deselected`() =
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
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val photo1UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo1Id
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.plusMonths(2)
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photo2UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo2Id
            }
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

            underTest.uiState.test {
                awaitItem()
                underTest.onDeselectAllPhotos()

                expectNoEvents()
            }
        }

    @Test
    fun `test that the correct bottom bar actions are set`() = runTest {
        val mockPhoto = mock<PhotoUiState.Image> {
            on { id } doReturn 12L
        }
        val node = mock<PhotoNodeUiState> {
            on { photo } doReturn mockPhoto
        }

        underTest.onPhotoSelected(node)

        underTest.actionUiState.test {
            assertThat(expectMostRecentItem().selectionModeItem.bottomBarActions).isEqualTo(
                persistentListOf(
                    TimelineSelectionMenuAction.Download,
                    TimelineSelectionMenuAction.ShareLink,
                    TimelineSelectionMenuAction.SendToChat,
                    TimelineSelectionMenuAction.Share,
                    TimelineSelectionMenuAction.MoveToRubbishBin,
                    TimelineSelectionMenuAction.More
                )
            )
        }
    }

    @Test
    fun `test that the remove link action menu is available when a single exported node is selected`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult = PhotoResult(
                photo = photo,
                isMarkedSensitive = false
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val exportedDataMock = mock<ExportedData>()
            val typedNode = mock<TypedNode> {
                on { id } doReturn NodeId(longValue = photoId)
                on { exportedData } doReturn exportedDataMock
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(NodeId(longValue = photoId))
                )
            ) doReturn listOf(typedNode)
            val photoUiState = mock<PhotoUiState.Image> {
                on { id } doReturn photoId
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo)
            ) doReturn photoUiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult)
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

            underTest.uiState.test {
                val selectedPhoto =
                    awaitItem().displayedPhotos.find { it is PhotosNodeContentType.PhotoNodeItem }
                underTest.onPhotoSelected((selectedPhoto as PhotosNodeContentType.PhotoNodeItem).node)
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().selectionModeItem.bottomSheetActions).contains(
                    TimelineSelectionMenuAction.RemoveLink
                )
            }
        }

    @Test
    fun `test that the remove link action menu is not available when multiple nodes are selected`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
            }
            val photoResult1 = PhotoResult(
                photo = photo1,
                isMarkedSensitive = false
            )
            val mockTextFileTypeInfo = mock<TextFileTypeInfo>()
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockTextFileTypeInfo
            }
            val photoResult2 = PhotoResult(
                photo = photo2,
                isMarkedSensitive = false
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult1, photoResult2)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val exportedDataMock1 = mock<ExportedData>()
            val typedNode1 = mock<TypedNode> {
                on { id } doReturn NodeId(photo1Id)
                on { exportedData } doReturn exportedDataMock1
            }
            val exportedDataMock2 = mock<ExportedData>()
            val typedNode2 = mock<TypedNode> {
                on { id } doReturn NodeId(photo2Id)
                on { exportedData } doReturn exportedDataMock2
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(
                        NodeId(longValue = photo1Id),
                        NodeId(longValue = photo2Id)
                    )
                )
            ) doReturn listOf(typedNode1, typedNode2)
            val photo1UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo1Id
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo1)
            ) doReturn photo1UiState
            val photo2UiState = mock<PhotoUiState.Image> {
                on { id } doReturn photo2Id
            }
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

            underTest.uiState.test {
                underTest.onSelectAllPhotos()
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                assertThat(expectMostRecentItem().selectionModeItem.bottomSheetActions).doesNotContain(
                    TimelineSelectionMenuAction.RemoveLink
                )
            }
        }

    @Test
    fun `test that the unhide menu action is available when node is hidden`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
                on { isSensitiveInherited } doReturn false
            }
            val photoResult = PhotoResult(
                photo = photo,
                isMarkedSensitive = true
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val photoUiState = mock<PhotoUiState.Image> {
                on { id } doReturn photoId
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo)
            ) doReturn photoUiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult)
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            val exportedDataMock = mock<ExportedData>()
            val typedNode = mock<TypedNode> {
                on { id } doReturn NodeId(longValue = photoId)
                on { exportedData } doReturn exportedDataMock
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(NodeId(longValue = photoId))
                )
            ) doReturn listOf(typedNode)
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()
            isHiddenNodesEnabledFlow.emit(true)

            underTest.uiState.test {
                underTest.onSelectAllPhotos()
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                val item = expectMostRecentItem()
                assertThat(item.selectionModeItem.bottomSheetActions).contains(
                    TimelineSelectionMenuAction.Unhide
                )
                assertThat(item.selectionModeItem.bottomSheetActions).doesNotContain(
                    TimelineSelectionMenuAction.Hide
                )
            }
        }

    @Test
    fun `test that the hide menu action is available when node is not hidden`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
                on { isSensitiveInherited } doReturn true
            }
            val photoResult = PhotoResult(
                photo = photo,
                isMarkedSensitive = false
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val photoUiState = mock<PhotoUiState.Image> {
                on { id } doReturn photoId
            }
            val exportedDataMock = mock<ExportedData>()
            val typedNode = mock<TypedNode> {
                on { id } doReturn NodeId(longValue = photoId)
                on { exportedData } doReturn exportedDataMock
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(NodeId(longValue = photoId))
                )
            ) doReturn listOf(typedNode)
            whenever(
                photoUiStateMapper.invoke(photo = photo)
            ) doReturn photoUiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult)
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
            isHiddenNodesEnabledFlow.emit(false)

            underTest.uiState.test {
                underTest.onSelectAllPhotos()
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                val item = expectMostRecentItem()
                assertThat(item.selectionModeItem.bottomSheetActions).contains(
                    TimelineSelectionMenuAction.Hide
                )
                assertThat(item.selectionModeItem.bottomSheetActions).doesNotContain(
                    TimelineSelectionMenuAction.Unhide
                )
            }
        }

    @Test
    fun `test that the move menu action is always available`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
                on { isSensitiveInherited } doReturn true
            }
            val photoResult = PhotoResult(
                photo = photo,
                isMarkedSensitive = false
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val photoUiState = mock<PhotoUiState.Image> {
                on { id } doReturn photoId
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo)
            ) doReturn photoUiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult)
            }
            whenever(
                monitorTimelinePhotosUseCase.sortPhotos(
                    photos = eq(emptyList()),
                    sortOrder = any()
                )
            ) doReturn sortResult
            val exportedDataMock = mock<ExportedData>()
            val typedNode = mock<TypedNode> {
                on { id } doReturn NodeId(longValue = photoId)
                on { exportedData } doReturn exportedDataMock
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(NodeId(longValue = photoId))
                )
            ) doReturn listOf(typedNode)
            whenever(
                photosNodeListCardMapper.invoke(photosDateResults = any())
            ) doReturn persistentListOf()
            isHiddenNodesEnabledFlow.emit(false)

            underTest.uiState.test {
                underTest.onSelectAllPhotos()
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                val item = expectMostRecentItem()
                assertThat(item.selectionModeItem.bottomSheetActions).contains(
                    TimelineSelectionMenuAction.Move
                )
            }
        }

    @Test
    fun `test that the copy menu action is always available`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
                on { isSensitiveInherited } doReturn true
            }
            val photoResult = PhotoResult(
                photo = photo,
                isMarkedSensitive = false
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val photoUiState = mock<PhotoUiState.Image> {
                on { id } doReturn photoId
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo)
            ) doReturn photoUiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult)
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
            val exportedDataMock = mock<ExportedData>()
            val typedNode = mock<TypedNode> {
                on { id } doReturn NodeId(longValue = photoId)
                on { exportedData } doReturn exportedDataMock
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(NodeId(longValue = photoId))
                )
            ) doReturn listOf(typedNode)
            isHiddenNodesEnabledFlow.emit(false)

            underTest.uiState.test {
                underTest.onSelectAllPhotos()
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                val item = expectMostRecentItem()
                assertThat(item.selectionModeItem.bottomSheetActions).contains(
                    TimelineSelectionMenuAction.Copy
                )
            }
        }

    @Test
    fun `test that the add to album menu action is available when the selected node is an image node`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<SvgFileTypeInfo>()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
                on { isSensitiveInherited } doReturn true
            }
            val photoResult = PhotoResult(
                photo = photo,
                isMarkedSensitive = false
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val photoUiState = mock<PhotoUiState.Image> {
                on { id } doReturn photoId
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo)
            ) doReturn photoUiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult)
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
            val typedNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = photoId)
                on { type } doReturn mockFileTypeInfo
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(NodeId(longValue = photoId))
                )
            ) doReturn listOf(typedNode)
            isHiddenNodesEnabledFlow.emit(false)

            underTest.uiState.test {
                underTest.onSelectAllPhotos()
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                val item = expectMostRecentItem()
                assertThat(item.selectionModeItem.bottomSheetActions).contains(
                    TimelineSelectionMenuAction.AddToAlbum
                )
            }
        }

    @Test
    fun `test that the add to album menu action is available when the selected node is a video node`() =
        runTest {
            val now = LocalDateTime.now()
            val mockFileTypeInfo = mock<VideoFileTypeInfo>()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now
                on { fileTypeInfo } doReturn mockFileTypeInfo
                on { isSensitiveInherited } doReturn true
            }
            val photoResult = PhotoResult(
                photo = photo,
                isMarkedSensitive = false
            )
            val photosResult = mock<TimelinePhotosResult> {
                on { allPhotos } doReturn listOf(photoResult)
                on { nonSensitivePhotos } doReturn emptyList()
            }
            whenever(
                monitorTimelinePhotosUseCase.invoke(request = any())
            ) doReturn flowOf(photosResult)
            val photoUiState = mock<PhotoUiState.Image> {
                on { id } doReturn photoId
            }
            whenever(
                photoUiStateMapper.invoke(photo = photo)
            ) doReturn photoUiState
            val sortResult = mock<TimelineSortedPhotosResult> {
                on { sortedPhotos } doReturn listOf(photoResult)
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
            val typedNode = mock<TypedFileNode> {
                on { id } doReturn NodeId(longValue = photoId)
                on { type } doReturn mockFileTypeInfo
            }
            whenever(
                getNodeListByIdsUseCase(
                    nodeIds = listOf(NodeId(longValue = photoId))
                )
            ) doReturn listOf(typedNode)
            isHiddenNodesEnabledFlow.emit(false)

            underTest.uiState.test {
                underTest.onSelectAllPhotos()
                cancelAndIgnoreRemainingEvents()
            }

            underTest.actionUiState.test {
                val item = expectMostRecentItem()
                assertThat(item.selectionModeItem.bottomSheetActions).contains(
                    TimelineSelectionMenuAction.AddToAlbum
                )
            }
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
}
