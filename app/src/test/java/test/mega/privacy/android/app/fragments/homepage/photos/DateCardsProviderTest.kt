package test.mega.privacy.android.app.fragments.homepage.photos

import android.text.Spanned
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.fragment.GroupingLevel
import mega.privacy.android.app.utils.LogUtil
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.TextUtil
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime

class DateCardsProviderTest {
    private val previewFolder = File("path")

    @Test
    fun `test that all lists are empty if input is empty`() {
        val underTest = DateCardsProvider()

        underTest.extractCardsFromNodeList(previewFolder, emptyList())

        assertThat(underTest.getDays()).isEmpty()
        assertThat(underTest.getMonths()).isEmpty()
        assertThat(underTest.getYears()).isEmpty()
        assertThat(underTest.getNodesWithoutPreview()).isEmpty()
    }

    @Test
    fun `test that a card is returned for every day`() {
        val underTest = DateCardsProvider()

        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
    }

    @Test
    fun `test that only one month card is returned if all dates are in the same month`() {
        val underTest = DateCardsProvider()

        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears = numberOfYears, numberOfMonthsPerYear = numberOfMonthsPerYear, numberOfDaysPerMonth = numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that a card is returned for every month`() {
        val underTest = DateCardsProvider()

        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 6
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that only one year card is returned if all dates are in the same year`() {
        val underTest = DateCardsProvider()


        val numberOfYears = 1
        val numberOfMonthsPerYear = 6
        val numberOfDaysPerMonth = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getYears()).hasSize(numberOfYears)
    }

    private fun getNodes(numberOfYears: Int, numberOfMonthsPerYear: Int, numberOfDaysPerMonth: Int): List<MegaNode> {
        val offset = OffsetDateTime.now().offset

        val nodes =
                (1..numberOfYears).map { year ->
                    (1..numberOfMonthsPerYear).map { month ->
                        (1..numberOfDaysPerMonth).map { day ->
                            LocalDateTime.of(year, month, day, 1, 1).toEpochSecond(offset)
                        }
                    }.flatten()
                }.flatten()
                        .map { epochSeconds ->
                            mock<MegaNode> { on { modificationTime }.thenReturn(epochSeconds) }
                        }
        return nodes
    }

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