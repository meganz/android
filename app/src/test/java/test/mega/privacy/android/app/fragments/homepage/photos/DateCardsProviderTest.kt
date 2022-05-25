package test.mega.privacy.android.app.fragments.homepage.photos

import android.text.Spanned
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.fragment.GroupingLevel
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime

class DateCardsProviderTest() {
    private val previewFolder = File("path")

    private lateinit var underTest: DateCardsProvider

    private val fileUtilWrapper = mock<FileUtilWrapper>()

    @Before
    fun setUp() {
        underTest = DateCardsProvider(fileUtil = fileUtilWrapper)
    }

    @Test
    fun `test that all lists are empty if input is empty`() {
        underTest.extractCardsFromNodeList(previewFolder, emptyList())

        assertThat(underTest.getDays()).isEmpty()
        assertThat(underTest.getMonths()).isEmpty()
        assertThat(underTest.getYears()).isEmpty()
        assertThat(underTest.getNodesWithoutPreview()).isEmpty()
    }

    @Test
    fun `test that a card is returned for every day`() {

        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
    }

    @Test
    fun `test that multiple dates on the same day increases the item count`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val numberOfDuplicateDays = 20
        val nodes = (1..numberOfDuplicateDays).map {
            getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)
        }.flatten()

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        val days = underTest.getDays()
        assertThat(days).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
        assertThat(days.last().numItems).isEqualTo(numberOfDuplicateDays - 1)
    }

    @Test
    fun `test that only one month card is returned if all dates are in the same month`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears = numberOfYears, numberOfMonthsPerYear = numberOfMonthsPerYear, numberOfDaysPerMonth = numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that a card is returned for every month`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 6
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that only one year card is returned if all dates are in the same year`() {
        val numberOfYears = 1
        val numberOfMonthsPerYear = 6
        val numberOfDaysPerMonth = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getYears()).hasSize(numberOfYears)
    }

    @Test
    fun `test missing preview on one day`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(1)
        assertThat(nodesWithoutPreview.keys.first().base64Handle).isEqualTo(getHandleString(1,1,1))
    }

    @Test
    fun `test non missing preview on one day`() {
        whenever(fileUtilWrapper.getFileIfExists(any(), any())).thenReturn(File("Exists"))

        val numberOfYears = 1
        val numberOfDaysPerMonth = 2
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(0)
    }

    @Test
    fun `test missing preview on duplicate day is not in missing preview list`() {
        whenever(fileUtilWrapper.getFileIfExists(any(), any())).thenReturn(File("Exists"), null)

        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val numberOfDuplicateDays = 2
        val nodes = (1..numberOfDuplicateDays).map {
            getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)
        }.flatten()

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(0)
    }

    @Test
    fun `test missing previews on multiple days`() {
        val numberOfYears = 2
        val numberOfDaysPerMonth = 3
        val numberOfMonthsPerYear = 4
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
        assertThat(nodesWithoutPreview.keys.first().base64Handle).isEqualTo(getHandleString(1,1,1))
        assertThat(nodesWithoutPreview.keys.last().base64Handle).isEqualTo(getHandleString(3,4,2))
    }

    private fun getNodes(numberOfYears: Int, numberOfMonthsPerYear: Int, numberOfDaysPerMonth: Int): List<MegaNode> {
        val offset = OffsetDateTime.now().offset

        val nodes =
                (1..numberOfYears).map { year ->
                    (1..numberOfMonthsPerYear).map { month ->
                        (1..numberOfDaysPerMonth).map { day ->
                            LocalDateTime.of(year, month, day, 1, 1)
                        }
                    }.flatten()
                }.flatten()
                        .map { localDateTime ->
                            val day = localDateTime.dayOfMonth
                            val month = localDateTime.monthValue
                            val year = localDateTime.year
                            mock<MegaNode> {
                                on { base64Handle }.thenReturn(getHandleString(day, month, year))
                                on { modificationTime }.thenReturn(localDateTime.toEpochSecond(offset))
                            }
                        }
        return nodes
    }

    private fun getHandleString(day: Int, month: Int, year: Int) =
            "Day: $day Month:$month Year:$year"


    private fun GalleryCard.dateString(level: GroupingLevel): String {

        val date = when (level) {
            GroupingLevel.Years -> "[B]$year[/B]" //Pair(year!!, "") // year -> [B]year[/B]
            GroupingLevel.Months -> if (year == null) "[B]$month[/B]" //Pair(month!!, "") //month -> [B]month[/B]
            else StringResourcesUtils.getString(
                    R.string.cu_month_year_date,
                    month,
                    year
            ) // [B]month[/B] year -> [B][/B] [B]month[/B] year
            GroupingLevel.Days -> if (year == null) "[B]$date[/B]" // Pair(date, "") // date -> [B]date[/B]
            else StringResourcesUtils.getString(
                    R.string.cu_day_month_year_date,
                    day,
                    month,
                    year
            ) // [B]day month[/B] year -> [B][/B] [B] day month [/B] year
            else -> ""// Pair("", "")// [B][/B]
        }

        return date
    }

    private fun spanString(stringToSpan: String): Spanned {
        return stringToSpan.runCatching {
            replace("[B]", "<font face=\"sans-serif-medium\">")
                    .replace("[/B]", "</font>")
        }.fold(
                onSuccess = { it.toSpannedHtmlText() },
                onFailure = {
                    Timber.e(it, "Exception formatting text.")
                    stringToSpan.toSpannedHtmlText()
                }
        )
    }

}