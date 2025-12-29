package mega.privacy.android.domain.usecase.photos

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.SortOrder
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
import mega.privacy.android.domain.usecase.node.hiddennode.MonitorHiddenNodesEnabledUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import java.text.SimpleDateFormat
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
    private val getTimelinePhotosUseCase: GetTimelinePhotosUseCase,
    private val monitorShowHiddenItemsUseCase: MonitorShowHiddenItemsUseCase,
    private val monitorHiddenNodesEnabledUseCase: MonitorHiddenNodesEnabledUseCase,
    private val getTimelineFilterPreferencesUseCase: GetTimelineFilterPreferencesUseCase,
    private val getCloudDrivePhotos: FilterCloudDrivePhotos,
    private val getCameraUploadPhotos: FilterCameraUploadPhotos,
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
        return combine(
            flow = getTimelinePhotosUseCase().distinctUntilChanged(),
            flow2 = monitorShowHiddenItemsUseCase().distinctUntilChanged(),
            flow3 = monitorHiddenNodesEnabledUseCase().distinctUntilChanged(),
            flow4 = request.selectedFilterFlow.distinctUntilChanged()
        ) { photos, showHiddenItems, isHiddenNodesEnabled, newFilter ->
            processTimelinePhotos(
                photos = photos,
                showHiddenItems = showHiddenItems,
                isHiddenNodesEnabled = isHiddenNodesEnabled,
                shouldApplyFilterFromPreference = newFilter != null
            )
        }.flowOn(defaultDispatcher)
    }

    private suspend fun processTimelinePhotos(
        photos: List<Photo>,
        showHiddenItems: Boolean?,
        isHiddenNodesEnabled: Boolean,
        shouldApplyFilterFromPreference: Boolean,
    ): TimelinePhotosResult {
        val allPhotos = photos.map { photo ->
            photo.toPhotoResult(isHiddenNodesEnabled = isHiddenNodesEnabled)
        }
        val nonSensitivePhotos = photos.filterPhotos(
            isHiddenNodesEnabled = isHiddenNodesEnabled,
            shouldApplyFilterFromPreference = shouldApplyFilterFromPreference,
            showHiddenItems = showHiddenItems ?: true,
        )
        return TimelinePhotosResult(
            allPhotos = allPhotos,
            nonSensitivePhotos = nonSensitivePhotos
        )
    }

    private suspend fun List<Photo>.filterPhotos(
        isHiddenNodesEnabled: Boolean,
        shouldApplyFilterFromPreference: Boolean,
        showHiddenItems: Boolean,
    ): List<PhotoResult> {
        val preferences = runCatching {
            getTimelineFilterPreferencesUseCase()
        }.getOrNull()
        val arePreferencesRemembered = preferences
            ?.get(TimelinePreferencesJSON.JSON_KEY_REMEMBER_PREFERENCES.value)
            .toBoolean()
        val shouldFilterMedia = arePreferencesRemembered || shouldApplyFilterFromPreference
        val mediaSource = if (shouldFilterMedia) {
            preferences?.get(TimelinePreferencesJSON.JSON_KEY_LOCATION.value)
                ?: TimelinePreferencesJSON.JSON_VAL_LOCATION_ALL_LOCATION.value
        } else null
        val mediaType = if (shouldFilterMedia) {
            preferences?.get(TimelinePreferencesJSON.JSON_KEY_MEDIA_TYPE.value)
                ?: TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_ALL_MEDIA.value
        } else null
        val shouldSkipNonSensitiveCheck = showHiddenItems || !isHiddenNodesEnabled
        var mediaFilteredResult: List<Photo> = this
        // Filter by media source
        mediaFilteredResult = when (mediaSource) {
            TimelinePreferencesJSON.JSON_VAL_LOCATION_CLOUD_DRIVE.value -> {
                getCloudDrivePhotos(source = mediaFilteredResult)
            }

            TimelinePreferencesJSON.JSON_VAL_LOCATION_CAMERA_UPLOAD.value -> {
                getCameraUploadPhotos(source = mediaFilteredResult)
            }

            else -> mediaFilteredResult
        }
        // Filter by media type
        mediaFilteredResult = when (mediaType) {
            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_IMAGES.value -> {
                mediaFilteredResult.filterIsInstance<Photo.Image>()
            }

            TimelinePreferencesJSON.JSON_VAL_MEDIA_TYPE_VIDEOS.value -> {
                mediaFilteredResult.filterIsInstance<Photo.Video>()
            }

            else -> mediaFilteredResult
        }

        return mediaFilteredResult
            .asSequence()
            .filter { photo ->
                // We don't directly use the photo.isMarkedSensitive here because photo.isMarkedSensitive
                // is used to display non sensitive photos but with sensitive design.
                val isNotSensitive = !photo.isSensitive && !photo.isSensitiveInherited
                shouldSkipNonSensitiveCheck || isNotSensitive
            }
            .map { photo ->
                photo.toPhotoResult(isHiddenNodesEnabled = isHiddenNodesEnabled)
            }
            .toList()
    }

    private fun Photo.toPhotoResult(isHiddenNodesEnabled: Boolean): PhotoResult {
        return PhotoResult(
            photo = this,
            isMarkedSensitive = isHiddenNodesEnabled && (this.isSensitive || this.isSensitiveInherited)
        )
    }

    suspend fun sortPhotos(
        photos: List<PhotoResult>,
        sortOrder: SortOrder,
    ): TimelineSortedPhotosResult =
        withContext(defaultDispatcher) {
            val sortedPhotos = when (sortOrder) {
                SortOrder.ORDER_MODIFICATION_DESC -> {
                    photos.sortedWith(compareByDescending<PhotoResult> { it.photo.modificationTime }.thenByDescending { it.photo.id })
                }

                SortOrder.ORDER_MODIFICATION_ASC -> {
                    photos.sortedWith(compareBy<PhotoResult> { it.photo.modificationTime }.thenByDescending { it.photo.id })
                }

                else -> photos
            }
            val nowYear = Year.now()
            val dayPhotos = groupPhotosByDay(sortedPhotos = sortedPhotos)
            val photosInYear = async { createYearsCardList(dayPhotos = dayPhotos) }
            val photosInMonth = async {
                createMonthsCardList(
                    dayPhotos = dayPhotos,
                    nowYear = nowYear
                )
            }
            val photosInDay = async { createDaysCardList(dayPhotos = dayPhotos, nowYear = nowYear) }
            TimelineSortedPhotosResult(
                sortedPhotos = sortedPhotos,
                photosInDay = photosInDay.await(),
                photosInMonth = photosInMonth.await(),
                photosInYear = photosInYear.await()
            )
        }

    private fun groupPhotosByDay(sortedPhotos: List<PhotoResult>): Map<PhotoResult, Int> {
        if (sortedPhotos.isEmpty()) return emptyMap()

        val map = LinkedHashMap<Long, Pair<PhotoResult, Int>>()
        for (photo in sortedPhotos) {
            val key = photo.photo.modificationTime.toLocalDate().toEpochDay()
            val current = map[key]
            if (current == null) {
                map[key] = photo to 1
            } else {
                map[key] = current.first to current.second + 1
            }
        }
        return map.values.associate { it }
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

    private fun createMonthsCardList(
        dayPhotos: Map<PhotoResult, Int>,
        nowYear: Year,
    ): List<PhotoDateResult> =
        dayPhotos.keys
            .distinctBy { YearMonth.from(it.photo.modificationTime) }
            .map { createMonthCard(photo = it, nowYear = nowYear) }.toList()

    private fun createMonthCard(photo: PhotoResult, nowYear: Year): PhotoDateResult {
        val sameYear = nowYear == Year.from(photo.photo.modificationTime)
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

    private fun createDaysCardList(
        dayPhotos: Map<PhotoResult, Int>,
        nowYear: Year,
    ): List<PhotoDateResult> = dayPhotos.map { (photo, count) ->
        createDaysCard(photo = photo, photosCount = count, nowYear = nowYear)
    }

    private fun createDaysCard(
        photo: PhotoResult,
        photosCount: Int,
        nowYear: Year,
    ): PhotoDateResult {
        val sameYear = nowYear == Year.from(photo.photo.modificationTime)
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

    companion object Companion {
        private const val DATE_FORMAT_YEAR = "uuuu"
        private const val DATE_FORMAT_YEAR_WITH_MONTH = "yyyy"
        private const val DATE_FORMAT_MONTH = "LLLL"
        private const val DATE_FORMAT_DAY = "dd"
        private const val DATE_FORMAT_MONTH_WITH_DAY = "MMMM"
    }
}
