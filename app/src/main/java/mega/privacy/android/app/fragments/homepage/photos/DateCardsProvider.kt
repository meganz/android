package mega.privacy.android.app.fragments.homepage.photos

import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.extension.previewPath
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Tool class used to organize MegaNode list by years, months and days.
 * MegaNode will be converted GalleryCard object.
 * Also will filter out the node which doesn't have local preview.
 */
class DateCardsProvider(
    private val previewFolder: File,
    private val fileUtil: FileUtilWrapper = object : FileUtilWrapper {},
) {

    private val DATE_FORMAT_YEAR = "uuuu"

    private var dayNodes = mapOf<MediaItem, Long>()

    private interface MediaItem {
        val id: Long
        val modifiedDate: LocalDate
        val preview: File?
        val name: String
    }


    suspend fun processGalleryItems(nodes: List<GalleryItem>) {
        dayNodes = nodes.mapNotNull {
            it.node?.let { node ->
                val previewExists = fileUtil.getFileIfExists(previewFolder, node.previewPath)
                object : MediaItem {
                    override val id: Long = node.handle
                    override val modifiedDate: LocalDate = Util.fromEpoch(node.modificationTime)
                    override val preview: File? = previewExists
                    override val name: String = node.name
                }
            }
        }.groupBy { it.modifiedDate.toEpochDay() }
            .map { (_, list) ->
                list.minByOrNull { it.modifiedDate }!! to list.size - 1L
            }.toMap()
    }

    private fun createMonthCard(item: MediaItem): GalleryCard {
        val monthFormat = "LLLL"
        val yearFormat = "yyyy"
        val modifiedDate = item.modifiedDate
        val preview = item.preview
        val sameYear = Year.from(LocalDate.now()) == Year.from(modifiedDate)
        val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifiedDate)
        val month = SimpleDateFormat(monthFormat, Locale.getDefault()).format(
            Date.from(modifiedDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        )
        val monthCardDate = if (sameYear) {
            month
        } else {
            SimpleDateFormat("$monthFormat $yearFormat",
                Locale.getDefault()).format(
                Date.from(
                    modifiedDate.atStartOfDay()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                )
            )
        }
        return GalleryCard(
            id = item.id,
            name = item.name,
            preview = preview,
            day = null,
            month = month,
            year = if (sameYear) null else year,
            date = monthCardDate,
            localDate = modifiedDate,
            numItems = 0
        )
    }

    private fun createDayCard(item: MediaItem, itemCount: Long): GalleryCard {
        val dayFormat = "dd"
        val monthFormat = "MMMM"
        val modifiedDate = item.modifiedDate
        val preview = item.preview
        val day = DateTimeFormatter.ofPattern(dayFormat).format(modifiedDate)
        val monthForDayCard = DateTimeFormatter.ofPattern(monthFormat).format(modifiedDate)
        val sameYear = Year.from(LocalDate.now()) == Year.from(modifiedDate)
        val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifiedDate)
        val dayCardDate = DateTimeFormatter.ofPattern(
            if (sameYear) {
                "$dayFormat $monthFormat"
            } else {
                "$dayFormat $monthFormat $DATE_FORMAT_YEAR"
            }
        ).format(modifiedDate)
        return GalleryCard(
            id = item.id,
            name = item.name,
            preview = preview,
            day = day,
            month = monthForDayCard,
            year = if (sameYear) null else year,
            date = dayCardDate,
            localDate = modifiedDate,
            numItems = itemCount
        )
    }

    private fun createYearCard(item: MediaItem): GalleryCard {
        val modifiedDate = item.modifiedDate
        val preview = item.preview
        val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifiedDate)
        return GalleryCard(
            id = item.id,
            name = item.name,
            preview = preview,
            day = null,
            month = null,
            year = year,
            date = year,
            localDate = modifiedDate,
            numItems = 0
        )
    }

    fun getDays() = dayNodes.map { (key, value) ->
        createDayCard(key, value)
    }

    fun getMonths() = dayNodes.keys
        .sortedBy { it.modifiedDate }
        .distinctBy {
            YearMonth.from(it.modifiedDate)
        }.map { createMonthCard(it) }

    fun getYears() = dayNodes.keys
        .sortedBy { it.modifiedDate }
        .distinctBy { it.modifiedDate.year }
        .map { createYearCard(it) }

    val latestSortedMonths: List<GalleryCard>
        get() = dayNodes.keys
            .sortedByDescending { it.modifiedDate }
            .distinctBy {
                YearMonth.from(it.modifiedDate)
            }.map { createMonthCard(it) }

    val latestSortedYears: List<GalleryCard>
        get() = dayNodes.keys
            .sortedByDescending { it.modifiedDate }
            .distinctBy { it.modifiedDate.year }
            .map { createYearCard(it) }
}