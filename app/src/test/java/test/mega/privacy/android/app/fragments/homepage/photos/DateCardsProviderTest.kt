package test.mega.privacy.android.app.fragments.homepage.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File
import java.util.*

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
        assertThat(underTest.getDays()).hasSize(numberOfYears*numberOfMonthsPerYear*numberOfDaysPerMonth)
    }

    @Test
    fun `test that only one month card is returned if all dates are in the same month`() {
        val underTest = DateCardsProvider()

        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears*numberOfMonthsPerYear)
    }

    @Test
    fun `test that a card is returned for every month`() {
        val underTest = DateCardsProvider()

        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 6
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.extractCardsFromNodeList(previewFolder, nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears*numberOfMonthsPerYear)
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
        val cal = Calendar.getInstance()
        val nodes =
                (1..numberOfYears).map { year ->
                    (1..numberOfMonthsPerYear).map { month ->
                        (1..numberOfDaysPerMonth).map { day ->
                            cal.set(year, month, day)
                            cal.timeInMillis / 1000
                        }
                    }.flatten()
                }.flatten()
                        .map { epochSeconds ->
                            mock<MegaNode> { on { modificationTime }.thenReturn(epochSeconds) }
                        }
        return nodes
    }
}