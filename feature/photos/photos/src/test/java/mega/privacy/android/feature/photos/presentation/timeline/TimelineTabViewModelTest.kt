package mega.privacy.android.feature.photos.presentation.timeline

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.mapper.FileTypeIconMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePhotosResult
import mega.privacy.android.domain.entity.photos.TimelineSortedPhotosResult
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.photos.MonitorTimelinePhotosUseCase
import mega.privacy.android.feature.photos.mapper.PhotoUiStateMapper
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.timeline.mapper.PhotosNodeListCardMapper
import mega.privacy.android.feature_flags.AppFeatures
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TimelineTabViewModelTest {

    private lateinit var underTest: TimelineTabViewModel

    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase = mock()
    private val monitorTimelinePhotosUseCase: MonitorTimelinePhotosUseCase = mock()
    private val photoUiStateMapper: PhotoUiStateMapper = mock()
    private val fileTypeIconMapper: FileTypeIconMapper = mock()
    private val photosNodeListCardMapper: PhotosNodeListCardMapper = mock()

    @BeforeEach
    fun setup() {
        underTest = TimelineTabViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorTimelinePhotosUseCase = monitorTimelinePhotosUseCase,
            photoUiStateMapper = photoUiStateMapper,
            fileTypeIconMapper = fileTypeIconMapper,
            photosNodeListCardMapper = photosNodeListCardMapper
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            getFeatureFlagValueUseCase,
            monitorTimelinePhotosUseCase,
            photoUiStateMapper,
            fileTypeIconMapper,
            photosNodeListCardMapper
        )
    }

    @Test
    fun `test that feature flags and photos are fetched successfully`() = runTest {
        whenever(
            getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)
        ) doReturn true
        whenever(
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        ) doReturn false
        val now = LocalDateTime.now()
        val mockFileTypeInfo = mock<VideoFileTypeInfo>()
        val photo1 = mock<Photo.Image> {
            on { id } doReturn 1L
            on { modificationTime } doReturn now
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult1 = PhotoResult(photo = photo1, isMarkedSensitive = false)
        val photoUiState1 = mock<PhotoUiState.Image>()
        whenever(
            photoUiStateMapper.invoke(photo = photo1)
        ) doReturn photoUiState1
        val photo2 = mock<Photo.Video> {
            on { id } doReturn 2L
            on { modificationTime } doReturn now.minusDays(1)
            on { fileTypeInfo } doReturn mockFileTypeInfo
        }
        val photoResult2 = PhotoResult(photo = photo2, isMarkedSensitive = true)
        val photosResult = TimelinePhotosResult(
            allPhotos = listOf(photoResult1, photoResult2),
            nonSensitivePhotos = listOf(photoResult1)
        )
        val sortResult = mock<TimelineSortedPhotosResult> {
            on { sortedPhotos } doReturn emptyList()
        }
        whenever(
            monitorTimelinePhotosUseCase.sortPhotos(
                isPaginationEnabled = true,
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
            val item = expectMostRecentItem()
            assertThat(item.allPhotos.size).isEqualTo(2)
            assertThat(item.isPaginationEnabled).isTrue()
        }
        val expectedRequest = TimelinePhotosRequest(
            isPaginationEnabled = true,
            isHiddenNodesActive = false
        )
        verify(monitorTimelinePhotosUseCase).invoke(expectedRequest)
        verify(monitorTimelinePhotosUseCase).sortPhotos(
            isPaginationEnabled = true,
            photos = photosResult.nonSensitivePhotos,
            sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        )
    }

    @Test
    fun `test that the next page is not loaded when pagination is disabled`() = runTest {
        val isPaginationEnabled = false
        whenever(
            getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)
        ) doReturn isPaginationEnabled
        whenever(
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        ) doReturn false
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
                isPaginationEnabled = isPaginationEnabled,
                photos = emptyList(),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn sortResult
        whenever(
            photosNodeListCardMapper.invoke(photosDateResults = any())
        ) doReturn persistentListOf()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isPaginationEnabled).isFalse()
            underTest.loadNextPage()
        }
        verify(monitorTimelinePhotosUseCase, never()).loadNextPage()
    }

    @Test
    fun `test that the next page is loaded when pagination is enabled`() = runTest {
        val isPaginationEnabled = true
        whenever(
            getFeatureFlagValueUseCase(AppFeatures.TimelinePhotosPagination)
        ) doReturn isPaginationEnabled
        whenever(
            getFeatureFlagValueUseCase(ApiFeatures.HiddenNodesInternalRelease)
        ) doReturn false
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
                isPaginationEnabled = isPaginationEnabled,
                photos = emptyList(),
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )
        ) doReturn sortResult
        whenever(
            photosNodeListCardMapper.invoke(photosDateResults = any())
        ) doReturn persistentListOf()

        underTest.uiState.test {
            assertThat(expectMostRecentItem().isPaginationEnabled).isTrue()
            underTest.loadNextPage()
        }
        verify(monitorTimelinePhotosUseCase).loadNextPage()
    }

    @Test
    fun `test that all photos are resorted when the sort order changes`() = runTest {
        whenever(
            getFeatureFlagValueUseCase(any())
        ) doReturn false
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
                isPaginationEnabled = eq(false),
                photos = eq(emptyList()),
                sortOrder = any()
            )
        ) doReturn sortResult
        whenever(
            photosNodeListCardMapper.invoke(photosDateResults = any())
        ) doReturn persistentListOf()

        underTest.onSortOrderChange(value = SortOrder.ORDER_MODIFICATION_ASC)

        underTest.uiState.test {
            assertThat(
                expectMostRecentItem().currentSort
            ).isEqualTo(SortOrder.ORDER_MODIFICATION_ASC)
        }
        verify(monitorTimelinePhotosUseCase).sortPhotos(
            isPaginationEnabled = any(),
            photos = any(),
            sortOrder = eq(SortOrder.ORDER_MODIFICATION_ASC)
        )
    }
}
