package test.mega.privacy.android.app.presentation.photos.timeline.viewmodel

import app.cash.turbine.test
import com.google.common.truth.Correspondence
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeListByIds
import mega.privacy.android.app.presentation.photos.timeline.model.ApplyFilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.DateCard
import mega.privacy.android.app.presentation.photos.timeline.model.FilterMediaType
import mega.privacy.android.app.presentation.photos.timeline.model.PhotoListItem
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.timeline.model.TimeBarTab
import mega.privacy.android.app.presentation.photos.timeline.model.TimelinePhotosSource
import mega.privacy.android.app.presentation.photos.timeline.model.ZoomLevel
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel
import mega.privacy.android.app.utils.wrapper.JobUtilWrapper
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.usecase.EnablePhotosCameraUpload
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetTimelinePhotos
import mega.privacy.android.domain.usecase.IsCameraSyncPreferenceEnabled
import mega.privacy.android.domain.usecase.SetInitialCUPreferences
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {
    private lateinit var underTest: TimelineViewModel

    private val isCameraSyncPreferenceEnabled =
        mock<IsCameraSyncPreferenceEnabled> { on { invoke() }.thenReturn(true) }

    private val getTimelinePhotos =
        mock<GetTimelinePhotos> { on { invoke() }.thenReturn(emptyFlow()) }

    private val filterCameraUploadPhotos =
        mock<FilterCameraUploadPhotos> { onBlocking { invoke(any()) }.thenAnswer { it.arguments[0] } }

    private val filterCloudDrivePhotos =
        mock<FilterCloudDrivePhotos> { onBlocking { invoke(any()) }.thenAnswer { it.arguments[0] } }

    private val setInitialCUPreferences = mock<SetInitialCUPreferences>()

    private val enablePhotosCameraUpload = mock<EnablePhotosCameraUpload>()

    private val getNodeListByIds = mock<GetNodeListByIds> {
        onBlocking { invoke(any()) }.thenReturn(
            emptyList())
    }

    private val jobUtilWrapper =
        mock<JobUtilWrapper> { on { isOverQuota() }.thenReturn(false) }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = TimelineViewModel(
            isCameraSyncPreferenceEnabled = isCameraSyncPreferenceEnabled,
            getTimelinePhotos = getTimelinePhotos,
            getCameraUploadPhotos = filterCameraUploadPhotos,
            getCloudDrivePhotos = filterCloudDrivePhotos,
            setInitialCUPreferences = setInitialCUPreferences,
            enablePhotosCameraUpload = enablePhotosCameraUpload,
            getNodeListByIds = getNodeListByIds,
            jobUtilWrapper = jobUtilWrapper,
            ioDispatcher = StandardTestDispatcher(),
            mainDispatcher = StandardTestDispatcher(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() = runTest {
        underTest.state.test {
            val initialState = awaitItem()
            assertWithMessage("photos value is incorrect").that(initialState.photos).isEmpty()
            assertWithMessage("currentShowingPhotos value is incorrect").that(initialState.currentShowingPhotos)
                .isEmpty()
            assertWithMessage("photosListItems value is incorrect").that(initialState.photosListItems)
                .isEmpty()
            assertWithMessage("loadPhotosDone value is incorrect").that(initialState.loadPhotosDone)
                .isFalse()
            assertWithMessage("yearsCardPhotos value is incorrect").that(initialState.yearsCardPhotos)
                .isEmpty()
            assertWithMessage("monthsCardPhotos value is incorrect").that(initialState.monthsCardPhotos)
                .isEmpty()
            assertWithMessage("daysCardPhotos value is incorrect").that(initialState.daysCardPhotos)
                .isEmpty()
            assertWithMessage("timeBarTabs value is incorrect").that(initialState.timeBarTabs)
                .containsExactlyElementsIn(TimeBarTab.values())
            assertWithMessage("selectedTimeBarTab value is incorrect").that(initialState.selectedTimeBarTab)
                .isEqualTo(TimeBarTab.All)
            assertWithMessage("currentZoomLevel value is incorrect").that(initialState.currentZoomLevel)
                .isEqualTo(ZoomLevel.Grid_3)
            assertWithMessage("scrollStartIndex value is incorrect").that(initialState.scrollStartIndex)
                .isEqualTo(0)
            assertWithMessage("applyFilterMediaType value is incorrect").that(initialState.applyFilterMediaType)
                .isEqualTo(ApplyFilterMediaType.ALL_MEDIA_IN_CD_AND_CU)
            assertWithMessage("currentFilterMediaType value is incorrect").that(initialState.currentFilterMediaType)
                .isEqualTo(FilterMediaType.ALL_MEDIA)
            assertWithMessage("currentMediaSource value is incorrect").that(initialState.currentMediaSource)
                .isEqualTo(TimelinePhotosSource.ALL_PHOTOS)
            assertWithMessage("currentSort value is incorrect").that(initialState.currentSort)
                .isEqualTo(Sort.NEWEST)
            assertWithMessage("showingFilterPage value is incorrect").that(initialState.showingFilterPage)
                .isFalse()
            assertWithMessage("showingSortByDialog value is incorrect").that(initialState.showingSortByDialog)
                .isFalse()
            assertWithMessage("enableZoomIn value is incorrect").that(initialState.enableZoomIn)
                .isTrue()
            assertWithMessage("enableZoomOut value is incorrect").that(initialState.enableZoomOut)
                .isTrue()
            assertWithMessage("enableSortOption value is incorrect").that(initialState.enableSortOption)
                .isTrue()
            assertWithMessage("enableCameraUploadButtonShowing value is incorrect").that(
                initialState.enableCameraUploadButtonShowing).isTrue()
            assertWithMessage("progressBarShowing value is incorrect").that(initialState.progressBarShowing)
                .isFalse()
            assertWithMessage("progress value is incorrect").that(initialState.progress)
                .isEqualTo(0f)
            assertWithMessage("pending value is incorrect").that(initialState.pending).isEqualTo(0)
            assertWithMessage("enableCameraUploadPageShowing value is incorrect").that(initialState.enableCameraUploadPageShowing)
                .isFalse()
            assertWithMessage("cuUploadsVideos value is incorrect").that(initialState.cuUploadsVideos)
                .isFalse()
            assertWithMessage("cuUseCellularConnection value is incorrect").that(initialState.cuUseCellularConnection)
                .isFalse()
            assertWithMessage("selectedPhotoCount value is incorrect").that(initialState.selectedPhotoCount)
                .isEqualTo(0)
            assertWithMessage("selectedPhoto value is incorrect").that(initialState.selectedPhoto)
                .isNull()
        }
    }

    @Test
    fun `test that a single photo returned is returned by the state`() = runTest {
        val expectedDate = LocalDateTime.now()
        val photo = mock<Photo> { on { modificationTime }.thenReturn(expectedDate) }
        whenever(getTimelinePhotos()).thenReturn(flowOf(listOf(photo)))

        underTest.state.drop(1).test {
            val initialisedState = awaitItem()
            assertWithMessage("Expected photos do not match").that(initialisedState.photos)
                .containsExactly(photo)
            assertWithMessage("Expected photosListItems do not match").that(initialisedState.photosListItems)
                .containsExactlyElementsIn(
                    listOf(PhotoListItem.Separator(expectedDate),
                        PhotoListItem.PhotoGridItem(photo, false))
                )
            val hasPhoto =
                Correspondence.transforming<DateCard, Photo>({ it?.photo }, "contains photo")

            assertWithMessage("Day card photos do not match").that(initialisedState.daysCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Month card photos do not match").that(initialisedState.monthsCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Year card photos do not match").that(initialisedState.yearsCardPhotos)
                .comparingElementsUsing(hasPhoto)
                .contains(photo)

            assertWithMessage("Loading is not complete").that(initialisedState.loadPhotosDone).isTrue()
        }
    }
}