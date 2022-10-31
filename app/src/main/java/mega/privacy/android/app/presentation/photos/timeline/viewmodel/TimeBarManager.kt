package mega.privacy.android.app.presentation.photos.timeline.viewmodel

import kotlinx.coroutines.flow.update
import mega.privacy.android.app.presentation.photos.model.DateCard
import mega.privacy.android.app.presentation.photos.model.Sort
import mega.privacy.android.app.presentation.photos.model.TimeBarTab
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


internal fun TimelineViewModel.createYearsCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
    dayPhotos.keys.distinctBy { it.modificationTime.year }
        .map { createYearCard(it) }.toList()


private fun createYearCard(photo: Photo): DateCard {
    val year = DateTimeFormatter.ofPattern(TimelineViewModel.DATE_FORMAT_YEAR)
        .format(photo.modificationTime)

    return DateCard.YearsCard(
        date = year,
        photo = photo,
    )
}

internal fun TimelineViewModel.createMonthsCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
    dayPhotos.keys.distinctBy { YearMonth.from(it.modificationTime) }
        .map { createMonthCard(it) }.toList()

private fun createMonthCard(photo: Photo): DateCard {
    val sameYear = Year.from(LocalDate.now()) == Year.from(photo.modificationTime)
    val month = SimpleDateFormat(TimelineViewModel.DATE_FORMAT_MONTH, Locale.getDefault()).format(
        Date.from(photo.modificationTime.toLocalDate().atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant())
    )
    val showDate = if (sameYear) {
        month
    } else {
        SimpleDateFormat("${TimelineViewModel.DATE_FORMAT_MONTH} ${TimelineViewModel.DATE_FORMAT_YEAR_WITH_MONTH}",
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

internal fun TimelineViewModel.createDaysCardList(dayPhotos: Map<Photo, Int>): List<DateCard> =
    dayPhotos.map { (key, value) ->
        createDaysCard(key, value)
    }.toList()

private fun createDaysCard(photo: Photo, photosCount: Int): DateCard {
    val sameYear = Year.from(LocalDate.now()) == Year.from(photo.modificationTime)
    val showDate = DateTimeFormatter.ofPattern(
        if (sameYear) {
            "${TimelineViewModel.DATE_FORMAT_DAY} ${TimelineViewModel.DATE_FORMAT_MONTH_WITH_DAY}"
        } else {
            "${TimelineViewModel.DATE_FORMAT_DAY} ${TimelineViewModel.DATE_FORMAT_MONTH_WITH_DAY} ${TimelineViewModel.DATE_FORMAT_YEAR}"
        }
    ).format(photo.modificationTime)
    return DateCard.DaysCard(
        date = showDate,
        photo = photo,
        photosCount = photosCount.toString()
    )
}

fun TimelineViewModel.setDateCardStartIndex(index: Int) =
    _state.update {
        it.copy(scrollStartIndex = index)
    }

fun TimelineViewModel.onCardClick(dateCard: DateCard) {
    when (dateCard) {
        is DateCard.YearsCard -> {
            val monthsCardList = _state.value.monthsCardPhotos
            val photo = monthsCardList.find {
                it.photo.modificationTime == dateCard.photo.modificationTime
            }
            updateSelectedTimeBarState(TimeBarTab.Months, monthsCardList.indexOf(photo))
        }
        is DateCard.MonthsCard -> {
            val daysCardList = _state.value.daysCardPhotos
            val photo = daysCardList.find {
                it.photo.modificationTime == dateCard.photo.modificationTime
            }
            updateSelectedTimeBarState(TimeBarTab.Days, daysCardList.indexOf(photo))
        }
        is DateCard.DaysCard -> {
            val photosList = _state.value.photosListItems
            val photo = photosList.find {
                it.key == dateCard.photo.id.toString()
            }
            updateSelectedTimeBarState(
                TimeBarTab.All,
                photosList.indexOf(photo)
            )
        }
    }
}

fun TimelineViewModel.onTimeBarTabSelected(tab: TimeBarTab) {
    when (tab) {
        TimeBarTab.Years -> {
            updateSelectedTimeBarState(TimeBarTab.Years)
        }
        TimeBarTab.Months -> {
            updateSelectedTimeBarState(TimeBarTab.Months)
        }
        TimeBarTab.Days -> {
            updateSelectedTimeBarState(TimeBarTab.Days)
        }
        TimeBarTab.All -> {
            updateSelectedTimeBarState(TimeBarTab.All)
        }
    }
}