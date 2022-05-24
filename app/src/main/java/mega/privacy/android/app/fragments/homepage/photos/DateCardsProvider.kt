package mega.privacy.android.app.fragments.homepage.photos

import android.content.Context
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaNode
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Date

/**
 * Tool class used to organize MegaNode list by years, months and days.
 * MegaNode will be converted GalleryCard object.
 * Also will filter out the node which doesn't have local preview.
 */
class DateCardsProvider {

    companion object {
        private const val DATE_FORMAT_DAY = "dd"
        private const val DATE_FORMAT_MONTH = "MMMM"
        private const val DATE_FORMAT_MONTH_STANDALONE = "LLLL"
        private const val DATE_FORMAT_YEAR = "uuuu"
        private const val DATE_FORMAT_YEAR_OF_ERA = "yyyy"
    }

    /**
     * Days list.
     */
    private val days = mutableListOf<GalleryCard>()

    /**
     * Months list.
     */
    private val months = mutableListOf<GalleryCard>()

    /**
     * Years list.
     */
    private val years = mutableListOf<GalleryCard>()

    /**
     * MegaNodes which doesn't have local preview.
     * Key: MegaNode
     * Value: Local path of the preview file that will be downloaded.
     */
    private val nodesWithoutPreview = mutableMapOf<MegaNode, String>()

    /**
     * Organize MegaNode list by years, months and days.
     * Convert MegaNode to GalleryCard and fill corresponding list.
     *
     * @param context Context object for get preview files' local path.
     * @param nodes MegaNode list, from which date cards will be extracted.
     */
    fun extractCardsFromNodeList(previewFolder: File, nodes: List<MegaNode>) {
        var lastDayDate: LocalDate? = null
        var lastMonthDate: LocalDate? = null
        var lastYearDate: LocalDate? = null

        nodes.forEach foreach@{ node ->
            var shouldGetPreview = false
            val preview = File(
                previewFolder,
                node.base64Handle + FileUtil.JPG_EXTENSION
            )

            val modifyDate = Util.fromEpoch(node.modificationTime)
            val day = DateTimeFormatter.ofPattern(DATE_FORMAT_DAY).format(modifyDate)
            val month = SimpleDateFormat(DATE_FORMAT_MONTH_STANDALONE, Locale.getDefault()).format(
                Date.from(modifyDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
            )
            val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifyDate)
            val monthForDayCard = DateTimeFormatter.ofPattern(DATE_FORMAT_MONTH).format(modifyDate)
            val sameYear = Year.from(LocalDate.now()) == Year.from(modifyDate)

            if (lastDayDate == null || lastDayDate!!.dayOfYear != modifyDate.dayOfYear) {
                shouldGetPreview = true
                lastDayDate = modifyDate
                val date = DateTimeFormatter.ofPattern(
                    if (sameYear) {
                        "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH"
                    } else {
                        "$DATE_FORMAT_DAY $DATE_FORMAT_MONTH $DATE_FORMAT_YEAR"
                    }
                ).format(lastDayDate)
                days.add(
                    GalleryCard(
                        node, if (preview.exists()) preview else null, day, monthForDayCard,
                        if (sameYear) null else year, date, modifyDate, 0
                    )
                )
            } else if (days.isNotEmpty()) {
                days[days.size - 1].incrementNumItems()
            }

            if (lastMonthDate == null || YearMonth.from(lastMonthDate) != YearMonth.from(modifyDate)
            ) {
                shouldGetPreview = true
                lastMonthDate = modifyDate
                val date = if (sameYear) month else SimpleDateFormat("$DATE_FORMAT_MONTH_STANDALONE $DATE_FORMAT_YEAR_OF_ERA", Locale.getDefault()).format(
                    Date.from(modifyDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
                )
                months.add(
                    GalleryCard(
                        node, if (preview.exists()) preview else null, null, month,
                        if (sameYear) null else year, date, modifyDate, 0
                    )
                )
            }

            if (lastYearDate == null || Year.from(lastYearDate) != Year.from(modifyDate)) {
                shouldGetPreview = true
                lastYearDate = modifyDate
                years.add(
                    GalleryCard(
                        node, if (preview.exists()) preview else null, null, null,
                        year, year, modifyDate, 0
                    )
                )
            }

            if (shouldGetPreview && !preview.exists()) {
                nodesWithoutPreview[node] = preview.absolutePath
            }
        }
    }

    // Public getter functions
    fun getDays() = days.toList()
    fun getMonths() = months.toList()
    fun getYears() = years.toList()
    fun getNodesWithoutPreview() = nodesWithoutPreview.toMap()
}