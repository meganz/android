package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.AccountLevelDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoDateResult
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import kotlin.random.Random

@Suppress("UnusedFlow")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTimelinePhotosUseCaseTest {

    private lateinit var underTest: MonitorTimelinePhotosUseCase

    private val monitorPaginatedTimelinePhotosUseCase: MonitorPaginatedTimelinePhotosUseCase =
        mock()
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase = mock()
    private val loadNextPageOfPhotosUseCase: LoadNextPageOfPhotosUseCase = mock()
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase = mock()
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase = mock()
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase = mock()
    private val getCloudDrivePhotos: FilterCloudDrivePhotos = mock()
    private val getCameraUploadPhotos: FilterCameraUploadPhotos = mock()
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase = mock()
    private val getNodeByIdUseCase: GetNodeByIdUseCase = mock()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        runTest {
            whenever(getTimelineFilterPreferencesUseCase()) doReturn null
            whenever(getBusinessStatusUseCase()) doReturn BusinessAccountStatus.Active
        }

        underTest = MonitorTimelinePhotosUseCase(
            defaultDispatcher = dispatcher,
            ioDispatcher = dispatcher,
            monitorPaginatedTimelinePhotosUseCase = monitorPaginatedTimelinePhotosUseCase,
            getTimelinePhotosUseCase = getTimelinePhotosUseCase,
            loadNextPageOfPhotosUseCase = loadNextPageOfPhotosUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            getTimelineFilterPreferencesUseCase = getTimelineFilterPreferencesUseCase,
            getCloudDrivePhotos = getCloudDrivePhotos,
            getCameraUploadPhotos = getCameraUploadPhotos,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            monitorPaginatedTimelinePhotosUseCase,
            getTimelinePhotosUseCase,
            loadNextPageOfPhotosUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
            getTimelineFilterPreferencesUseCase,
            getCloudDrivePhotos,
            getCameraUploadPhotos,
            getBusinessStatusUseCase,
            getNodeByIdUseCase
        )
    }

    @Test
    fun `test that pagination source is returned when pagination is enabled`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest(isPaginationEnabled = true)
            val now = LocalDateTime.now()
            val photo1 = mock<Photo.Image> {
                on { id } doReturn 1
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val photo2 = mock<Photo.Image> {
                on { id } doReturn 2
                on { modificationTime } doReturn now.minusDays(2)
                on { isSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val photo3 = mock<Photo.Video> {
                on { id } doReturn 3
                on { modificationTime } doReturn now.minusDays(3)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn true
            }
            val allPhotosList = listOf(photo1, photo2, photo3)

            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_III
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            whenever(monitorPaginatedTimelinePhotosUseCase()) doReturn flowOf(allPhotosList)
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)

            underTest(request = request).test {
                assertThat(expectMostRecentItem().allPhotos.size).isEqualTo(3)
            }

            verify(monitorPaginatedTimelinePhotosUseCase, times(1)).invoke()
            verify(loadNextPageOfPhotosUseCase, times(1)).invoke()
            verify(getTimelinePhotosUseCase, times(0)).invoke()
        }

    @Test
    fun `test that non-pagination source is returned when pagination is not enabled`() = runTest(
        dispatcher
    ) {
        val request = TimelinePhotosRequest(isPaginationEnabled = false)
        val now = LocalDateTime.now()
        val photo1 = mock<Photo.Image> {
            on { id } doReturn 1
            on { modificationTime } doReturn now.minusDays(1)
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }
        val photo2 = mock<Photo.Image> {
            on { id } doReturn 2
            on { modificationTime } doReturn now.minusDays(2)
            on { isSensitive } doReturn true
            on { isSensitiveInherited } doReturn false
        }
        val photo3 = mock<Photo.Video> {
            on { id } doReturn 3
            on { modificationTime } doReturn now.minusDays(3)
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn true
        }
        val allPhotosList = listOf(photo1, photo2, photo3)
        val accountLevelDetail = mock<AccountLevelDetail> {
            on { accountType } doReturn AccountType.PRO_III
        }
        val accountDetail = mock<AccountDetail> {
            on { levelDetail } doReturn accountLevelDetail
        }

        whenever(getTimelinePhotosUseCase()) doReturn flowOf(allPhotosList)
        whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
        whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)

        underTest(request = request).test {
            assertThat(expectMostRecentItem().allPhotos.size).isEqualTo(3)
        }

        verify(getTimelinePhotosUseCase, times(1)).invoke()
        verify(monitorPaginatedTimelinePhotosUseCase, times(0)).invoke()
    }

    @Test
    fun `test that the photo is not marked as sensitive when hidden nodes flag is not active`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest(isPaginationEnabled = false)
            val now = LocalDateTime.now()
            val photo1 = mock<Photo.Image> {
                on { id } doReturn 1
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val allPhotosList = listOf(photo1)
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_III
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }

            whenever(getTimelinePhotosUseCase()) doReturn flowOf(allPhotosList)
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)

            underTest(request = request).test {
                val item = expectMostRecentItem()
                assertThat(item.allPhotos.none { it.isMarkedSensitive }).isTrue()
                assertThat(item.allPhotos.size).isEqualTo(item.nonSensitivePhotos.size)
            }
        }

    @Test
    fun `test that sensitive photos are successfully filtered when hidden nodes flag is active`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest(isPaginationEnabled = false)
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_III
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            val now = LocalDateTime.now()
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.minusDays(2)
                on { isSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val photo3Id = 3L
            val photo3 = mock<Photo.Video> {
                on { id } doReturn photo3Id
                on { modificationTime } doReturn now.minusDays(3)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn true
            }
            val allPhotosList = listOf(photo1, photo2, photo3)
            whenever(getTimelinePhotosUseCase()) doReturn flowOf(allPhotosList)
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(false)
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)

            underTest(request = request).test {
                val item = expectMostRecentItem()
                assertThat(item.allPhotos.find { it.photo.id == photo1Id }!!.isMarkedSensitive).isFalse()
                assertThat(item.allPhotos.find { it.photo.id == photo2Id }!!.isMarkedSensitive).isTrue()
                assertThat(item.allPhotos.find { it.photo.id == photo3Id }!!.isMarkedSensitive).isTrue()
                assertThat(item.nonSensitivePhotos.size).isEqualTo(1)
                assertThat(item.nonSensitivePhotos.first().photo.id).isEqualTo(photo1Id)
            }
        }

    @Test
    fun `test that sensitive photos are included as non-sensitive photos when should show hidden items`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest(isPaginationEnabled = false)
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_III
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            val now = LocalDateTime.now()
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.minusDays(2)
                on { isSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val photo3Id = 3L
            val photo3 = mock<Photo.Video> {
                on { id } doReturn photo3Id
                on { modificationTime } doReturn now.minusDays(3)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn true
            }
            val allPhotosList = listOf(photo1, photo2, photo3)
            whenever(getTimelinePhotosUseCase()) doReturn flowOf(allPhotosList)
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(true)
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)

            underTest(request = request).test {
                val item = expectMostRecentItem()
                assertThat(item.nonSensitivePhotos.size).isEqualTo(3)
            }
        }

    @Test
    fun `test that the original photos order is returned when pagination is enabled`() = runTest {
        val now = LocalDateTime.now()
        val photo1Id = 1L
        val photo1 = mock<Photo.Image> {
            on { id } doReturn photo1Id
            on { modificationTime } doReturn now.minusDays(1)
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }
        val photo2Id = 2L
        val photo2 = mock<Photo.Image> {
            on { id } doReturn photo2Id
            on { modificationTime } doReturn now.minusDays(2)
            on { isSensitive } doReturn true
            on { isSensitiveInherited } doReturn false
        }
        val unsortedPhotos = listOf(
            PhotoResult(
                photo = photo2,
                isMarkedSensitive = false,
                inTypedNode = null
            ),
            PhotoResult(
                photo = photo1,
                isMarkedSensitive = false,
                inTypedNode = null
            )
        )
        val isPaginationEnabled = true

        val actual = underTest.sortPhotos(
            isPaginationEnabled = isPaginationEnabled,
            photos = unsortedPhotos,
            sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        )

        assertThat(actual.sortedPhotos[0].photo.id).isEqualTo(photo2Id)
        assertThat(actual.sortedPhotos[1].photo.id).isEqualTo(photo1Id)
    }

    @Test
    fun `test that the list of photos is successfully sorted by the modification time in descending`() =
        runTest {
            val now = LocalDateTime.now()
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.minusDays(2)
                on { isSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val unsortedPhotos = listOf(
                PhotoResult(
                    photo = photo2,
                    isMarkedSensitive = false,
                    inTypedNode = null
                ),
                PhotoResult(
                    photo = photo1,
                    isMarkedSensitive = false,
                    inTypedNode = null
                )
            )
            val isPaginationEnabled = false

            val actual = underTest.sortPhotos(
                isPaginationEnabled = isPaginationEnabled,
                photos = unsortedPhotos,
                sortOrder = SortOrder.ORDER_MODIFICATION_DESC
            )

            assertThat(actual.sortedPhotos[0].photo.id).isEqualTo(photo1Id)
            assertThat(actual.sortedPhotos[1].photo.id).isEqualTo(photo2Id)
        }

    @Test
    fun `test that the list of photos is successfully sorted by the modification time in ascending`() =
        runTest {
            val now = LocalDateTime.now()
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.minusDays(2)
                on { isSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val unsortedPhotos = listOf(
                PhotoResult(
                    photo = photo2,
                    isMarkedSensitive = false,
                    inTypedNode = null
                ),
                PhotoResult(
                    photo = photo1,
                    isMarkedSensitive = false,
                    inTypedNode = null
                )
            )
            val isPaginationEnabled = false

            val actual = underTest.sortPhotos(
                isPaginationEnabled = isPaginationEnabled,
                photos = unsortedPhotos,
                sortOrder = SortOrder.ORDER_MODIFICATION_ASC
            )

            assertThat(actual.sortedPhotos[0].photo.id).isEqualTo(photo2Id)
            assertThat(actual.sortedPhotos[1].photo.id).isEqualTo(photo1Id)
        }

    @Test
    fun `test that sorted photos are correctly grouped by day, month, and year`() = runTest {
        val now = LocalDateTime.now()
        val photoToday = mock<Photo.Image> {
            on { id } doReturn 1
            on { modificationTime } doReturn now
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }
        val photoTodayResult = PhotoResult(
            photo = photoToday,
            isMarkedSensitive = false,
            inTypedNode = null
        )
        val photoLastMonth = mock<Photo.Image> {
            on { id } doReturn 2
            on { modificationTime } doReturn now.minusMonths(1)
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }
        val photoLastMonthResult = PhotoResult(
            photo = photoLastMonth,
            isMarkedSensitive = false,
            inTypedNode = null
        )
        val photoLastYear = mock<Photo.Image> {
            on { id } doReturn 3
            on { modificationTime } doReturn now.minusYears(1)
            on { isSensitive } doReturn false
            on { isSensitiveInherited } doReturn false
        }
        val photoLastYearResult = PhotoResult(
            photo = photoLastYear,
            isMarkedSensitive = false,
            inTypedNode = null
        )
        val photos = listOf(photoTodayResult, photoLastMonthResult, photoLastYearResult)

        val actual = underTest.sortPhotos(
            isPaginationEnabled = false,
            photos = photos,
            sortOrder = SortOrder.ORDER_MODIFICATION_DESC
        )

        assertThat(actual.photosInDay.size).isEqualTo(3)
        assertThat(actual.photosInMonth.size).isEqualTo(3)
        assertThat(actual.photosInYear.size).isEqualTo(2)
        assertThat(actual.photosInDay[0]).isInstanceOf(PhotoDateResult.Day::class.java)
        assertThat(actual.photosInMonth[0]).isInstanceOf(PhotoDateResult.Month::class.java)
        assertThat(actual.photosInYear[0]).isInstanceOf(PhotoDateResult.Year::class.java)
    }

    @Test
    fun `test that sensitive photos are successfully filtered with the new filter when hidden nodes flag is active`() =
        runTest(dispatcher) {
            val isRemembered = Random.nextBoolean()
            val mediaType = "images"
            val mediaSource = "cloudDrive"
            val newFilter = mapOf(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to isRemembered.toString(),
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to mediaType,
                TimelinePreferencesJSON.JSON_KEY_LOCATION.value to mediaSource,
            )
            whenever(getTimelineFilterPreferencesUseCase()) doReturn mapOf(
                TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value to "false",
                TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value to mediaType,
                TimelinePreferencesJSON.JSON_KEY_LOCATION.value to mediaSource,
            )
            val request = TimelinePhotosRequest(
                isPaginationEnabled = false,
                selectedFilterFlow = flowOf(newFilter)
            )
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_III
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            val now = LocalDateTime.now()
            val photo1Id = 1L
            val photo1 = mock<Photo.Image> {
                on { id } doReturn photo1Id
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            val photo2Id = 2L
            val photo2 = mock<Photo.Image> {
                on { id } doReturn photo2Id
                on { modificationTime } doReturn now.minusDays(2)
                on { isSensitive } doReturn true
                on { isSensitiveInherited } doReturn false
            }
            val photo3Id = 3L
            val photo3 = mock<Photo.Video> {
                on { id } doReturn photo3Id
                on { modificationTime } doReturn now.minusDays(3)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn true
            }
            val allPhotosList = listOf(photo1, photo2, photo3)
            whenever(getTimelinePhotosUseCase()) doReturn flowOf(allPhotosList)
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(false)
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)

            underTest(request = request).test { cancelAndConsumeRemainingEvents() }
            verify(getCloudDrivePhotos).invoke(source = allPhotosList)
        }

    @Test
    fun `test that the correct typed node is returned`() =
        runTest(dispatcher) {
            val request = TimelinePhotosRequest(isPaginationEnabled = false)
            val accountLevelDetail = mock<AccountLevelDetail> {
                on { accountType } doReturn AccountType.PRO_III
            }
            val accountDetail = mock<AccountDetail> {
                on { levelDetail } doReturn accountLevelDetail
            }
            val now = LocalDateTime.now()
            val photoId = 1L
            val photo = mock<Photo.Image> {
                on { id } doReturn photoId
                on { modificationTime } doReturn now.minusDays(1)
                on { isSensitive } doReturn false
                on { isSensitiveInherited } doReturn false
            }
            whenever(getTimelinePhotosUseCase()) doReturn flowOf(listOf(photo))
            whenever(monitorShowHiddenItemsUseCase()) doReturn flowOf(false)
            whenever(monitorAccountDetailUseCase()) doReturn flowOf(accountDetail)
            val typedNode = mock<TypedNode>()
            whenever(getNodeByIdUseCase(id = any())) doReturn typedNode

            underTest(request = request).test {
                assertThat(expectMostRecentItem().allPhotos[0].inTypedNode).isEqualTo(typedNode)
            }
        }
}
