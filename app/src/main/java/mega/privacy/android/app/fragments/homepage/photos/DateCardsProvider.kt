package mega.privacy.android.app.fragments.homepage.photos

import mega.privacy.android.app.gallery.data.GalleryCard
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
class DateCardsProvider(private val fileUtil:FileUtilWrapper = object : FileUtilWrapper{}) {

    companion object {
        private const val DATE_FORMAT_DAY = "dd"
        private const val DATE_FORMAT_MONTH = "MMMM"
        private const val DATE_FORMAT_MONTH_STANDALONE = "LLLL"
        private const val DATE_FORMAT_YEAR = "uuuu"
        private const val DATE_FORMAT_YEAR_OF_ERA = "yyyy"
    }

    private var dayNodes = listOf<Pair<MegaNode, Long>>()

    /**
     * Days list.
     */
    private var days = listOf<GalleryCard>()

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
    private var nodesWithoutPreview = mapOf<MegaNode, String>()

    /**
     * Organize MegaNode list by years, months and days.
     * Convert MegaNode to GalleryCard and fill corresponding list.
     *
     * @param context Context object for get preview files' local path.
     * @param nodes MegaNode list, from which date cards will be extracted.
     */
    fun extractCardsFromNodeList(previewFolder: File, nodes: List<MegaNode>) {
        var lastEpochDay: Long = 0
        var lastYearMonth: YearMonth = YearMonth.of(0,1)
        var lastYear: Year = Year.of(0)

        dayNodes = nodes.groupBy { Util.fromEpoch(it.modificationTime).toEpochDay() }
                .map { (_, list) ->
                    Pair(list.minByOrNull { it.modificationTime }!!, list.size - 1L)
                }

        nodesWithoutPreview = dayNodes.filter {
            getPreview(previewFolder, it.first) == null
        }.associate {
            Pair(it.first, File(previewFolder,
                    it.first.base64Handle + FileUtil.JPG_EXTENSION).absolutePath)
        }

        nodes.forEach foreach@{ node ->
            val preview = getPreview(previewFolder, node)

            val modifyDate = Util.fromEpoch(node.modificationTime)
            val month = SimpleDateFormat(DATE_FORMAT_MONTH_STANDALONE, Locale.getDefault()).format(
                    Date.from(modifyDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
            )
            val year = DateTimeFormatter.ofPattern(DATE_FORMAT_YEAR).format(modifyDate)
            val sameYear = Year.from(LocalDate.now()) == Year.from(modifyDate)

            val monthCardDate = if (sameYear) month else SimpleDateFormat("$DATE_FORMAT_MONTH_STANDALONE $DATE_FORMAT_YEAR_OF_ERA", Locale.getDefault()).format(
                    Date.from(modifyDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant())
            )

            val yearCardDate = year
            days = dayNodes.map {
                createDayCard(it.first, previewFolder, it.second)
            }


            if (lastYearMonth != YearMonth.from(modifyDate)
            ) {
                lastYearMonth = YearMonth.from(modifyDate)

                months.add(
                        GalleryCard(
                                node = node,
                                preview = preview,
                                day = null,
                                month = month,
                                year = if (sameYear) null else year,
                                date = monthCardDate,
                                localDate = modifyDate,
                                numItems = 0
                        )
                )
            }

            if (Year.from(lastYear) != Year.from(modifyDate)) {
                lastYear = Year.from(modifyDate)

                years.add(
                        GalleryCard(
                                node = node,
                                preview = preview,
                                day = null,
                                month = null,
                                year = year,
                                date = yearCardDate,
                                localDate = modifyDate,
                                numItems = 0
                        )
                )
            }

        }
    }

    private fun createDayCard(node: MegaNode, previewFolder: File, itemCount: Long): GalleryCard {
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

    private fun getPreview(previewFolder: File, node: MegaNode) =
            fileUtil.getFileIfExists(
                    previewFolder,
                    node.base64Handle + FileUtil.JPG_EXTENSION
            )

    // Public getter functions
    fun getDays() = days
    fun getMonths() = months
    fun getYears() = years
    fun getNodesWithoutPreview() = nodesWithoutPreview
}