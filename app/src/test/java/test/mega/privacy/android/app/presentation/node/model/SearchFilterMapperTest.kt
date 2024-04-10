package test.mega.privacy.android.app.presentation.node.model

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.mapper.SearchFilterMapper
import mega.privacy.android.domain.entity.search.SearchCategory
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchFilterMapperTest {
    private lateinit var underTest: SearchFilterMapper
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        underTest = SearchFilterMapper(context)
    }

    @Test
    fun `test that when search category is default all is displayed to the user`() {
        val actual = underTest(SearchCategory.ALL)
        Truth.assertThat(actual.name).isEqualTo(context.getString(R.string.all_view_button))
    }

    @Test
    fun `test that when search category is image then filter has the name image`() {
        val actual = underTest(SearchCategory.IMAGES)
        Truth.assertThat(actual.name).isEqualTo(context.getString(R.string.section_images))
    }

    @Test
    fun `test that when search category is video all then filter has the name video`() {
        val actual = underTest(SearchCategory.VIDEO)
        Truth.assertThat(actual.name).isEqualTo(context.getString(R.string.upload_to_video))
    }

    @Test
    fun `test that when search category is audio all then filter has the name audio`() {
        val actual = underTest(SearchCategory.AUDIO)
        Truth.assertThat(actual.name).isEqualTo(context.getString(R.string.upload_to_audio))
    }

    @Test
    fun `test that when search category is document all then filter has the name docs`() {
        val actual = underTest(SearchCategory.ALL_DOCUMENTS)
        Truth.assertThat(actual.name).isEqualTo(context.getString(R.string.section_documents))
    }

    @Test
    fun `test that when search category is pdf all then filter has the name pdf`() {
        val actual = underTest(SearchCategory.PDF)
        Truth.assertThat(actual.name).isEqualTo("Pdf")
    }

    @Test
    fun `test that when search category is presentation all then filter has the name presentation`() {
        val actual = underTest(SearchCategory.PRESENTATION)
        Truth.assertThat(actual.name).isEqualTo("Presentation")
    }

    @Test
    fun `test that when search category is spreadsheet all then filter has the name spreadsheet`() {
        val actual = underTest(SearchCategory.SPREADSHEET)
        Truth.assertThat(actual.name).isEqualTo("Spreadsheet")
    }

    @Test
    fun `test that when search category is other all then filter has the name other`() {
        val actual = underTest(SearchCategory.OTHER)
        Truth.assertThat(actual.name).isEqualTo("Other")
    }

    @Test
    fun `test that when search category is folder all then filter has the name folder`() {
        val actual = underTest(SearchCategory.FOLDER)
        Truth.assertThat(actual.name).isEqualTo("Folder")
    }
}