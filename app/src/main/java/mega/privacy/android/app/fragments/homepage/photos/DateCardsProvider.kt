package mega.privacy.android.app.fragments.homepage.photos

import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.extension.thumbnailPath
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import nz.mega.sdk.MegaNode
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Tool class used to organize MegaNode list by years, months and days.
 * MegaNode will be converted GalleryCard object.
 * Also will filter out the node which doesn't have local preview.
 */
class DateCardsProvider(
        private val previewFolder: File,
        private val fileUtil: FileUtilWrapper = object : FileUtilWrapper {}) {

    private val DATE_FORMAT_YEAR = "uuuu"

    private var dayNodes = mapOf<MegaNode, Long>()


    fun processNodes(nodes: List<MegaNode>) {
        dayNodes = nodes.groupBy { Util.fromEpoch(it.modificationTime).toEpochDay() }
                .map { (_, list) ->
                    list.minByOrNull { it.modificationTime }!! to list.size - 1L
                }.toMap()
    }

    private fun identifyMissingPreviews(list: List<MegaNode>) = list.filter {
        getPreview(previewFolder, it) == null
    }.associateWith { node ->
        File(previewFolder,
                node.base64Handle + FileUtil.JPG_EXTENSION
        ).absolutePath
    }

    private fun createMonthCard(node: MegaNode, previewFolder: File): GalleryCard {
        val DATE_FORMAT_MONTH_STANDALONE = "LLLL"
        val DATE_FORMAT_YEAR_OF_ERA = "yyyy"
        val modifiedDate = Util.fromEpoch(node.modificationTime)
        val preview = getPreview(previewFolder, node)
        val sameYear = Year.from(LocalDate.now()) == Year.from(modifiedDate)
        val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifiedDate)
        val month = SimpleDateFormat(DATE_FORMAT_MONTH_STANDALONE, Locale.getDefault()).format(
                Date.from(modifiedDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
        )
        val monthCardDate = if (sameYear) {
            month
        } else {
            SimpleDateFormat("$DATE_FORMAT_MONTH_STANDALONE $DATE_FORMAT_YEAR_OF_ERA",
                    Locale.getDefault()).format(
                    Date.from(
                            modifiedDate.atStartOfDay()
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                    )
            )
        }
        return GalleryCard(
                node = node,
                preview = preview,
                day = null,
                month = month,
                year = if (sameYear) null else year,
                date = monthCardDate,
                localDate = modifiedDate,
                numItems = 0
        )
    }

    private fun createDayCard(node: MegaNode, previewFolder: File, itemCount: Long): GalleryCard {
        val DATE_FORMAT_DAY = "dd"
        val DATE_FORMAT_MONTH = "MMMM"
        val modifiedDate = Util.fromEpoch(node.modificationTime)
        val preview = getPreview(previewFolder, node)
        val day = DateTimeFormatter.ofPattern(DATE_FORMAT_DAY).format(modifiedDate)
        val monthForDayCard = DateTimeFormatter.ofPattern(DATE_FORMAT_MONTH).format(modifiedDate)
        val sameYear = Year.from(LocalDate.now()) == Year.from(modifiedDate)
        val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifiedDate)
        val dayCardDate = DateTimeFormatter.ofPattern(
                if (sameYear) {
                    "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH"
                } else {
                    "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH $DATE_FORMAT_YEAR"
                }
        ).format(modifiedDate)
        return GalleryCard(
                node = node,
                preview = preview,
                day = day,
                month = monthForDayCard,
                year = if (sameYear) null else year,
                date = dayCardDate,
                localDate = modifiedDate,
                numItems = itemCount
        )
    }

    private fun createYearCard(node: MegaNode, previewFolder: File): GalleryCard {
        val modifiedDate = Util.fromEpoch(node.modificationTime)
        val preview = getPreview(previewFolder, node)
        val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifiedDate)
        return GalleryCard(
                node = node,
                preview = preview,
                day = null,
                month = null,
                year = year,
                date = year,
                localDate = modifiedDate,
                numItems = 0
        )
    }

    private fun getPreview(previewFolder: File, node: MegaNode) =
            fileUtil.getFileIfExists(
                    previewFolder,
                    node.thumbnailPath
            )

    fun getDays() = dayNodes.map { (key, value) ->
        createDayCard(key, previewFolder, value)
    }

    fun getMonths() = dayNodes.keys
            .sortedBy { it.modificationTime }
            .distinctBy {
                YearMonth.from(Util.fromEpoch(it.modificationTime))
            }.map { createMonthCard(it, previewFolder) }

    fun getYears() = dayNodes.keys
            .sortedBy { it.modificationTime }
            .distinctBy { Util.fromEpoch(it.modificationTime).year }
            .map { createYearCard(it, previewFolder) }

    fun getNodesWithoutPreview() = identifyMissingPreviews(dayNodes.keys.toList())
}