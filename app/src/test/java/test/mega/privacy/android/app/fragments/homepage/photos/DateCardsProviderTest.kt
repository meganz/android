package test.mega.privacy.android.app.fragments.homepage.photos

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.data.MediaType
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime

class DateCardsProviderTest() {
    private val previewFolder = File("path")

    private lateinit var underTest: DateCardsProvider

    private val fileUtilWrapper = mock<FileUtilWrapper>()

    @Before
    fun setUp() {
        underTest = DateCardsProvider(previewFolder = previewFolder, fileUtil = fileUtilWrapper)
    }

    @Test
    fun `test that all lists are empty if input is empty`() {
        underTest.processGalleryItems(emptyList())

        assertThat(underTest.getDays()).isEmpty()
        assertThat(underTest.getMonths()).isEmpty()
        assertThat(underTest.getYears()).isEmpty()
    }

    @Test
    fun `test that a card is returned for every day`() {

        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val items = getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processGalleryItems(items)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
    }

    @Test
    fun `test that multiple dates on the same day increases the item count`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val numberOfDuplicateDays = 20
        val items = (1..numberOfDuplicateDays).map {
            getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)
        }.flatten()

        underTest.processGalleryItems(items)
        val days = underTest.getDays()
        assertThat(days).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
        assertThat(days.last().numItems).isEqualTo(numberOfDuplicateDays - 1)
    }

    @Test
    fun `test that only one month card is returned if all dates are in the same month`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val items = getGalleryItems(numberOfYears = numberOfYears, numberOfMonthsPerYear = numberOfMonthsPerYear, numberOfDaysPerMonth = numberOfDaysPerMonth)

        underTest.processGalleryItems(items)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that a card is returned for every month`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 6
        val items = getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processGalleryItems(items)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that only one year card is returned if all dates are in the same year`() {
        val numberOfYears = 1
        val numberOfMonthsPerYear = 6
        val numberOfDaysPerMonth = 1
        val items = getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processGalleryItems(items)
        assertThat(underTest.getYears()).hasSize(numberOfYears)
    }

    @Test
    fun `test that a year card is returned for every year`() {
        val numberOfYears = 4
        val numberOfMonthsPerYear = 6
        val numberOfDaysPerMonth = 4
        val items = getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processGalleryItems(items)
        assertThat(underTest.getYears()).hasSize(numberOfYears)
    }


    private fun getGalleryItems(numberOfYears: Int, numberOfMonthsPerYear: Int, numberOfDaysPerMonth: Int, identifier: Int? = null): List<GalleryItem> {
        val offset = OffsetDateTime.now().offset

        val items =
                (1..numberOfYears).map { year ->
                    (1..numberOfMonthsPerYear).map { month ->
                        (1..numberOfDaysPerMonth).map { day ->
                            LocalDateTime.of(year, month, day, 12, 0)
                        }
                    }.flatten()
                }.flatten()
                        .map { localDateTime ->
                            val day = localDateTime.dayOfMonth
                            val month = localDateTime.monthValue
                            val year = localDateTime.year
                            val node = mock<MegaNode> {
                                on { name }.thenReturn(getHandleString(day, month, year))
                                on { base64Handle }.thenReturn(getHandleString(day, month, year) + appendIdentifier(identifier))
                                on { modificationTime }.thenReturn(localDateTime.toEpochSecond(offset))
                            }
                            GalleryItem(node, 1, 1, null, MediaType.Header, "", null, null, false, false)
                        }
        return items
    }

    private fun getHandleString(day: Int, month: Int, year: Int) =
            "Day: $day Month:$month Year:$year"

    private fun appendIdentifier(identifier: Int?) = identifier?.let { " ($it)" } ?: ""

}