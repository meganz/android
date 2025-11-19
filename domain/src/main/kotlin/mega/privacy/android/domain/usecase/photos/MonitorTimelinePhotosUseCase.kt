package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.account.business.BusinessAccountStatus
import mega.privacy.android.domain.entity.photos.Photo
import mega.privacy.android.domain.entity.photos.PhotoDateResult
import mega.privacy.android.domain.entity.photos.PhotoResult
import mega.privacy.android.domain.entity.photos.TimelinePhotosRequest
import mega.privacy.android.domain.entity.photos.TimelinePhotosResult
import mega.privacy.android.domain.entity.photos.TimelinePreferencesJSON
import mega.privacy.android.domain.entity.photos.TimelineSortedPhotosResult
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.usecase.FilterCameraUploadPhotos
import mega.privacy.android.domain.usecase.FilterCloudDrivePhotos
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class MonitorTimelinePhotosUseCase @Inject constructor(
    @DefaultDispatcher val defaultDispatcher: CoroutineDispatcher,
    @IoDispatcher val ioDispatcher: CoroutineDispatcher,
    private val monitorPaginatedTimelinePhotosUseCase: MonitorPaginatedTimelinePhotosUseCase,
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val loadNextPageOfPhotosUseCase: LoadNextPageOfPhotosUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorAccountDetailUseCase: MonitorAccountDetailUseCase,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val getCloudDrivePhotos: FilterCloudDrivePhotos,
    private val getCameraUploadPhotos: FilterCameraUploadPhotos,
    private val getBusinessStatusUseCase: GetBusinessStatusUseCase,
) {

    private val dayMonthDateFormatter =
        DateTimeFormatter.ofPattern("$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY")
    private val dayMonthYearDateFormatter =
        DateTimeFormatter.ofPattern("$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY $DATE_FORMAT_YEAR")
    private val yearDateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR)
    private val monthDateFormatter = SimpleDateFormat(
        DATE_FORMAT_MONTH,
        Locale.getDefault()
    )
    private val monthYearDateFormatter = SimpleDateFormat(
        "$DATE_FORMAT_MONTH $DATE_FORMAT_YEAR_WITH_MONTH",
        Locale.getDefault()
    )

    operator fun invoke(request: TimelinePhotosRequest): Flow<TimelinePhotosResult> {
        val timelinePhotoSource =
            getTimelinePhotoSource(isPaginationEnabled = request.isPaginationEnabled)
        val showHiddenItemsSource = monitorShowHiddenItemsUseCase()
        val accountDetailSource = monitorAccountDetailUseCase()

        return combine(
            flow = timelinePhotoSource,
            flow2 = showHiddenItemsSource,
            flow3 = accountDetailSource
        ) { photos, showHiddenItems, accountDetail ->
            photos.toGetAndMonitorTimelinePhotosResult(
                showHiddenItems = showHiddenItems,
                accountDetail = accountDetail,
            )
        }.flowOn(ioDispatcher)
    }

    private suspend fun List<Photo>.toGetAndMonitorTimelinePhotosResult(
        showHiddenItems: Boolean?,
        accountDetail: AccountDetail?,
    ): TimelinePhotosResult = withContext(defaultDispatcher) {
        val accountType = accountDetail?.levelDetail?.accountType
        val isPaidAccount = accountType?.isPaid
        val businessStatus = if (accountType?.isBusinessAccount == true) {
            getBusinessStatusUseCase()
        } else null
        val shouldBeMarkedSensitive =
            accountDetail != null && isPaidAccount == true && businessStatus != BusinessAccountStatus.Expired
        val allPhotos =
            this@toGetAndMonitorTimelinePhotosResult.toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
        val filteredByMediaType = filterByMediaType(
            photos = allPhotos,
            shouldBeMarkedSensitive = shouldBeMarkedSensitive
        )
        val nonSensitivePhotos = filterNonSensitivePhotos(
            showHiddenItems = showHiddenItems,
            photos = filteredByMediaType,
            isPaidAccount = isPaidAccount,
            businessStatus = businessStatus
        )
        TimelinePhotosResult(
            allPhotos = allPhotos,
            nonSensitivePhotos = nonSensitivePhotos
        )
    }

    private suspend fun filterByMediaType(
        shouldBeMarkedSensitive: Boolean,
        photos: List<PhotoResult>,
    ): List<PhotoResult> {
        return runCatching {
            getTimelineFilterPreferencesUseCase()?.let { preferences ->
                val arePreferencesRemembered =
                    preferences[TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value].toBoolean()
                if (arePreferencesRemembered) {
                    val mediaType = preferences[TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value]
                        ?: TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
                    val mediaSource = preferences[TimelinePreferencesJSON.JSON_KEY_LOCATION.value]
                        ?: TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
                    filterPhotosByMediaType(
                        mediaType = mediaType,
                        mediaSource = mediaSource,
                        shouldBeMarkedSensitive = shouldBeMarkedSensitive,
                        photos = photos
                    )
                } else {
                    photos
                }
            } ?: run { photos }
        }.getOrElse { photos }
    }

    private fun getTimelinePhotoSource(isPaginationEnabled: Boolean): Flow<List<Photo>> {
        return if (isPaginationEnabled) {
            monitorPaginatedTimelinePhotosUseCase().onStart { loadNextPage() }
        } else {
            getTimelinePhotosUseCase()
        }
    }

    private suspend fun filterPhotosByMediaType(
        mediaType: String,
        mediaSource: String,
        shouldBeMarkedSensitive: Boolean,
        photos: List<PhotoResult>,
    ): List<PhotoResult> = when (mediaType) {
        TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value -> {
            when (mediaSource) {
                TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value -> {
                    photos
                }

                TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value -> {
                    getCloudDrivePhotos(source = photos.map { it.photo })
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value -> {
                    getCameraUploadPhotos(source = photos.map { it.photo })
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                else -> photos
            }
        }

        TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value -> {
            when (mediaSource) {
                TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value -> {
                    photos
                        .map { it.photo }
                        .filterIsInstance<Photo.Image>()
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value -> {
                    getCloudDrivePhotos(source = photos.map { it.photo })
                        .filterIsInstance<Photo.Image>()
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value -> {
                    getCameraUploadPhotos(source = photos.map { it.photo })
                        .filterIsInstance<Photo.Image>()
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                else -> photos
            }
        }

        TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value -> {
            when (mediaSource) {
                TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value -> {
                    photos
                        .map { it.photo }
                        .filterIsInstance<Photo.Video>()
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value -> {
                    getCloudDrivePhotos(source = photos.map { it.photo })
                        .filterIsInstance<Photo.Video>()
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value -> {
                    getCameraUploadPhotos(source = photos.map { it.photo })
                        .filterIsInstance<Photo.Video>()
                        .toPhotoResult(shouldBeMarkedSensitive = shouldBeMarkedSensitive)
                }

                else -> photos
            }
        }

        else -> photos
    }

    private fun List<Photo>.toPhotoResult(shouldBeMarkedSensitive: Boolean) = map {
        PhotoResult(
            photo = it,
            isMarkedSensitive = shouldBeMarkedSensitive && (it.isSensitive || it.isSensitiveInherited)
        )
    }

    private fun filterNonSensitivePhotos(
        photos: List<PhotoResult>,
        showHiddenItems: Boolean?,
        isPaidAccount: Boolean?,
        businessStatus: BusinessAccountStatus?,
    ): List<PhotoResult> {
        val showHiddenItems = showHiddenItems ?: return photos
        val isPaid = isPaidAccount ?: return photos
        return if (showHiddenItems || !isPaid || businessStatus == BusinessAccountStatus.Expired) {
            photos
        } else {
            // We don't directly use the it.isMarkedSensitive here because it.isMarkedSensitive
            // is used to display non sensitive photos but with sensitive design.
            photos.filter { !it.photo.isSensitive && !it.photo.isSensitiveInherited }
        }
    }

    suspend fun sortPhotos(
        isPaginationEnabled: Boolean,
        photos: List<PhotoResult>,
        sortOrder: SortOrder,
    ): TimelineSortedPhotosResult =
        withContext(defaultDispatcher) {
            val sortedPhotos = if (isPaginationEnabled) {
                photos
            } else {
                when (sortOrder) {
                    SortOrder.ORDER_MODIFICATION_DESC -> {
                        photos.sortedWith(compareByDescending<PhotoResult> { it.photo.modificationTime }.thenByDescending { it.photo.id })
                    }

                    SortOrder.ORDER_MODIFICATION_ASC -> {
                        photos.sortedWith(compareBy<PhotoResult> { it.photo.modificationTime }.thenByDescending { it.photo.id })
                    }

                    else -> photos
                }
            }
            val dayPhotos = groupPhotosByDay(sortedPhotos = sortedPhotos)
            val photosInYear = async { createYearsCardList(dayPhotos = dayPhotos) }
            val photosInMonth = async { createMonthsCardList(dayPhotos = dayPhotos) }
            val photosInDay = async { createDaysCardList(dayPhotos = dayPhotos) }
            TimelineSortedPhotosResult(
                sortedPhotos = sortedPhotos,
                photosInDay = photosInDay.await(),
                photosInMonth = photosInMonth.await(),
                photosInYear = photosInYear.await()
            )
        }

    private fun groupPhotosByDay(sortedPhotos: List<PhotoResult>): Map<PhotoResult, Int> {
        if (sortedPhotos.isEmpty()) return emptyMap()

        return sortedPhotos
            .groupingBy { it.photo.modificationTime.toLocalDate().toEpochDay() }
            .aggregate { _, accumulator: Pair<PhotoResult, Int>?, element, first ->
                if (first) {
                    element to 1
                } else {
                    accumulator!!.copy(second = accumulator.second + 1)
                }
            }
            .values
            .toMap()
    }

    private fun createYearsCardList(dayPhotos: Map<PhotoResult, Int>): List<PhotoDateResult> =
        dayPhotos.keys
            .distinctBy { it.photo.modificationTime.year }
            .map { createYearCard(it) }


    private fun createYearCard(photo: PhotoResult): PhotoDateResult {
        val year = yearDateFormatter.format(photo.photo.modificationTime)
        return PhotoDateResult.Year(
            date = year,
            photo = photo,
        )
    }

    private fun createMonthsCardList(dayPhotos: Map<PhotoResult, Int>): List<PhotoDateResult> =
        dayPhotos.keys
            .distinctBy { YearMonth.from(it.photo.modificationTime) }
            .map { createMonthCard(it) }.toList()

    private fun createMonthCard(photo: PhotoResult): PhotoDateResult {
        val sameYear = Year.from(LocalDate.now()) == Year.from(photo.photo.modificationTime)
        val startDate = Date.from(
            photo.photo.modificationTime.toLocalDate().atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant()
        )
        val date = if (sameYear) {
            monthDateFormatter.format(startDate)
        } else {
            monthYearDateFormatter.format(startDate)
        }
        return PhotoDateResult.Month(
            date = date,
            photo = photo
        )
    }

    private fun createDaysCardList(dayPhotos: Map<PhotoResult, Int>): List<PhotoDateResult> =
        dayPhotos.map { (photo, count) -> createDaysCard(photo = photo, photosCount = count) }

    private fun createDaysCard(photo: PhotoResult, photosCount: Int): PhotoDateResult {
        val sameYear = Year.from(LocalDate.now()) == Year.from(photo.photo.modificationTime)
        val date = if (sameYear) {
            dayMonthDateFormatter.format(photo.photo.modificationTime)
        } else {
            dayMonthYearDateFormatter.format(photo.photo.modificationTime)
        }
        return PhotoDateResult.Day(
            date = date,
            photo = photo,
            photosCount = photosCount
        )
    }


    suspend fun loadNextPage() {
        loadNextPageOfPhotosUseCase()
    }

    companion object Companion {
        private const val DATE_FORMAT_YEAR = "uuuu"
        private const val DATE_FORMAT_YEAR_WITH_MONTH = "yyyy"
        private const val DATE_FORMAT_MONTH = "LLLL"
        private const val DATE_FORMAT_DAY = "dd"
        private const val DATE_FORMAT_MONTH_WITH_DAY = "MMMM"
    }
}
