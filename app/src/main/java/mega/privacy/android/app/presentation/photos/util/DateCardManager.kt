package mega.privacy.android.app.presentation.photos.util

import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.domain.entity.photos.Photo
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

internal fun groupPhotosByDay(photos: List<Photo>, currentSort: Sort) =
    photos
        .groupBy { it.modificationTime.toLocalDate().toEpochDay() }
        .map { (_, photosList) ->
            if (currentSort == Sort.OLDEST)
                photosList.minByOrNull { it.modificationTime }!! to photosList.size
            else
                photosList.maxByOrNull { it.modificationTime }!! to photosList.size
        }
        .toMap()


internal fun createYearsCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
    dayPhotos.keys.distinctBy { it.modificationTime.year }
        .map { createYearCard(it) }.toList()


private fun createYearCard(photo: Photo): DateCard {
    val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR)
        .format(photo.modificationTime)

    return DateCard.YearsCard(
        date = year,
        photo = photo,
    )
}

internal fun createMonthsCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
    dayPhotos.keys.distinctBy { YearMonth.from(it.modificationTime) }
        .map { createMonthCard(it) }.toList()

private fun createMonthCard(photo: Photo): DateCard {
    val sameYear = Year.from(LocalDate.now()) == Year.from(photo.modificationTime)
    val month = SimpleDateFormat(DATE_FORMAT_MONTH, Locale.getDefault()).format(
        Date.from(photo.modificationTime.toLocalDate().atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant())
    )
    val showDate = if (sameYear) {
        month
    } else {
        SimpleDateFormat("$DATE_FORMAT_MONTH $DATE_FORMAT_YEAR_WITH_MONTH",
            Locale.getDefault()).format(
            Date.from(
                photo.modificationTime.toLocalDate().atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
            )
        )
    }
    return DateCard.MonthsCard(
        date = showDate,
        photo = photo
    )
}

internal fun createDaysCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
    dayPhotos.map { (key, value) ->
        createDaysCard(key, value)
    }.toList()

private fun createDaysCard(photo: Photo, photosCount: Int): DateCard {
    val sameYear = Year.from(LocalDate.now()) == Year.from(photo.modificationTime)
    val showDate = DateTimeFormatter.ofPattern(
        if (sameYear) {
            "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY"
        } else {
            "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH_WITH_DAY $DATE_FORMAT_YEAR"
        }
    ).format(photo.modificationTime)
    return DateCard.DaysCard(
        date = showDate,
        photo = photo,
        photosCount = photosCount.toString()
    )
}


