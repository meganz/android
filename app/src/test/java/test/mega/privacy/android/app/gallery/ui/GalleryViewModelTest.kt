@file:OptIn(ExperimentalCoroutinesApi::class)

package test.mega.privacy.android.app.gallery.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.repository.GalleryItemRepository
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.NodeSortOrder
import nz.mega.sdk.MegaNode
import org.junit.*
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.time.OffsetDateTime

class GalleryViewModelTest {
    private lateinit var underTest: GalleryViewModel

    private val galleryItemRepository = mock<GalleryItemRepository>()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = object : GalleryViewModel(
                galleryItemRepository = galleryItemRepository,
                sortOrderManagement = mock<NodeSortOrder>(),
                ioDispatcher = UnconfinedTestDispatcher(),
                onNodesChange = emptyFlow(),
                savedStateHandle = null,
        ) {
            override var mZoom: Int = 1
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Ignore("Fix linking errors by removing m ega dependencies")
    @Test
    fun `test missing preview on one day`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val items = getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        whenever(galleryItemRepository.galleryItems).thenReturn(MutableLiveData(items))

        underTest.dateCards.test()

        verifyBlocking(galleryItemRepository) { getPreviews(argWhere { it.size == 1 && it.first().name == getHandleString(1, 1, 1) }, any()) }
    }

//    @Test
//    fun `test non missing preview on one day`() {
//        whenever(fileUtilWrapper.getFileIfExists(any(), any())).thenReturn(File("Exists"))
//
//        val numberOfYears = 1
//        val numberOfDaysPerMonth = 2
//        val numberOfMonthsPerYear = 1
//        val items = getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)
//
//        underTest.processGalleryItems(items)
//        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
//
//        val itemsWithoutPreviews = underTest.getGalleryItemsWithoutPreviews()
//        assertThat(itemsWithoutPreviews.size).isEqualTo(0)
//    }
//
//    @Test
//    fun `test missing preview on duplicate day is not in missing preview list`() {
//        whenever(fileUtilWrapper.getFileIfExists(any(), argForWhich { contains("(1)") })).thenReturn(File("Exists"))
//        whenever(fileUtilWrapper.getFileIfExists(any(), argForWhich { !contains("(1)") })).thenReturn(null)
//
//        val numberOfYears = 1
//        val numberOfDaysPerMonth = 1
//        val numberOfMonthsPerYear = 1
//        val numberOfDuplicateDays = 2
//        val items = (1..numberOfDuplicateDays).map {
//            getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth, it)
//        }.flatten()
//
//        items.forEach { println(it.node?.base64Handle) }
//
//        underTest.processGalleryItems(items)
//        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
//
//        val itemsWithoutPreviews = underTest.getGalleryItemsWithoutPreviews()
//        assertThat(itemsWithoutPreviews.size).isEqualTo(0)
//    }
//
//    @Test
//    fun `test missing previews on multiple days`() {
//        val numberOfYears = 2
//        val numberOfDaysPerMonth = 3
//        val numberOfMonthsPerYear = 4
//        val items = getGalleryItems(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)
//
//        underTest.processGalleryItems(items)
//        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
//
//        val itemsWithoutPreviews = underTest.getGalleryItemsWithoutPreviews()
//        assertThat(itemsWithoutPreviews.size).isEqualTo(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
//        assertThat(itemsWithoutPreviews.keys.first().base64Handle).isEqualTo(getHandleString(1, 1, 1))
//        assertThat(itemsWithoutPreviews.keys.last().base64Handle).isEqualTo(getHandleString(3, 4, 2))
//    }

    private fun getGalleryItems(numberOfYears: Int, numberOfMonthsPerYear: Int, numberOfDaysPerMonth: Int, identifier: Int? = null): List<GalleryItem> {
        val offset = OffsetDateTime.now().offset

        val items =
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
                            val node = mock<MegaNode> {
                                on { name }.thenReturn(getHandleString(day, month, year))
                                on { base64Handle }.thenReturn(getHandleString(day, month, year) + appendIdentifier(identifier))
                                on { modificationTime }.thenReturn(localDateTime.toEpochSecond(offset))
                            }
                            GalleryItem(node, 1, 1, null, 1, "", null, null, false, false)
                        }
        return items
    }

    private fun getHandleString(day: Int, month: Int, year: Int) =
            "Day: $day Month:$month Year:$year"

    private fun appendIdentifier(identifier: Int?) = identifier?.let { " ($it)" } ?: ""
}