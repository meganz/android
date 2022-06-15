package test.mega.privacy.android.app.gallery.adapter

import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.databinding.ItemGalleryCardBinding
import mega.privacy.android.app.gallery.adapter.GalleryCardViewHolder
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.fragment.GroupingLevel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class GalleryCardViewHolderTest {

    private val applicationContext = ApplicationProvider.getApplicationContext<Context>()
    private val parent = RecyclerView(applicationContext).apply {
        layoutManager = GridLayoutManager(applicationContext, 1)
    }
    private lateinit var binding: ItemGalleryCardBinding

    private val day = "day"
    private val month = "month"
    private val year = "year"

    private val date = "date"

    private fun getCard(
        dayString: String = day,
        monthString: String = month,
        yearString: String? = year,
    ) = GalleryCard(
        id = 1L,
        name = "$day$month$year",
        preview = null,
        day = dayString,
        month = monthString,
        year = yearString,
        date = date,
        localDate = LocalDate.now()
    )

    @Before
    fun setUp() {
        Fresco.initialize(applicationContext)
        binding =
            ItemGalleryCardBinding.inflate(LayoutInflater.from(applicationContext), parent, false)
    }

    @After
    fun tearDown() {
        Fresco.shutDown()
    }

    @Test
    fun `test that year card date is formatted correctly`() {
        val underTest = GalleryCardViewHolder(
            GroupingLevel.Years.ordinal,
            binding,
            800,
            8
        )

        val expectedFormat = "$year"
        underTest.bind(getCard(), mock())

        assertThat(binding.dateText.text.toString()).isEqualTo(expectedFormat)
    }

    @Test
    fun `test that month card date is formatted correctly`() {
        val underTest = GalleryCardViewHolder(
            GroupingLevel.Months.ordinal,
            binding,
            800,
            8
        )

        val expectedFormat = "$month $year"
        underTest.bind(getCard(), mock())

        assertThat(binding.dateText.text.toString()).isEqualTo(expectedFormat)
    }

    @Test
    fun `test that month card date is formatted correctly if year is null`() {
        val underTest = GalleryCardViewHolder(
            GroupingLevel.Months.ordinal,
            binding,
            800,
            8
        )

        val expectedFormat = "$month"
        underTest.bind(getCard(yearString = null), mock())

        assertThat(binding.dateText.text.toString()).isEqualTo(expectedFormat)
    }

    @Test
    fun `test that day card date is formatted correctly`() {
        val underTest = GalleryCardViewHolder(
            GroupingLevel.Days.ordinal,
            binding,
            800,
            8
        )

        val expectedFormat = "$day $month, $year"
        underTest.bind(getCard(), mock())

        assertThat(binding.dateText.text.toString()).isEqualTo(expectedFormat)

    }

    @Test
    fun `test that day card date is formatted correctly if year is null`() {
        val underTest = GalleryCardViewHolder(
            GroupingLevel.Days.ordinal,
            binding,
            800,
            8
        )

        val expectedFormat = date
        underTest.bind(getCard(yearString = null), mock())

        assertThat(binding.dateText.text.toString()).isEqualTo(expectedFormat)

    }


}