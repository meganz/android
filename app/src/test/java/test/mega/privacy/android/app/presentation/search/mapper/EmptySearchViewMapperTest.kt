package test.mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.search.mapper.EmptySearchViewMapper
import mega.privacy.android.domain.entity.search.SearchCategory
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptySearchViewMapperTest {
    private lateinit var underTest: EmptySearchViewMapper
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        underTest = EmptySearchViewMapper(context)
    }

    @Test
    fun `test that when search query is not null no results description is shown to the user`() {
        val actual =
            underTest(
                isSearchChipEnabled = true,
                category = SearchCategory.ALL,
                searchQuery = "query"
            )
        Truth.assertThat(actual.second)
            .isEqualTo(context.getString(R.string.search_empty_screen_no_results))
    }

    @Test
    fun `test that when search query is null and user has not any chips then Cloud drive is empty message is shown to the user`() {
        val actual = underTest(
            isSearchChipEnabled = true,
            category = SearchCategory.ALL
        )
        Truth.assertThat(actual.second)
            .isEqualTo(context.getString(R.string.cloud_drive_empty_screen_message))
    }

    @Test
    fun `test that when search query is null and user has selected images then No images message is shown to the user`() {
        val actual = underTest(
            isSearchChipEnabled = true,
            category = SearchCategory.IMAGES
        )
        Truth.assertThat(actual.second)
            .isEqualTo(context.getString(R.string.search_empty_screen_no_images))
    }

    @Test
    fun `test that when search query is null and user has selected documents then No documents message is shown to the user`() {
        val actual = underTest(
            isSearchChipEnabled = true,
            category = SearchCategory.DOCUMENTS
        )
        Truth.assertThat(actual.second)
            .isEqualTo(context.getString(R.string.search_empty_screen_no_documents))
    }

    @Test
    fun `test that when search query is null and user has selected audio then No images message is shown to the user`() {
        val actual = underTest(
            isSearchChipEnabled = true,
            category = SearchCategory.AUDIO
        )
        Truth.assertThat(actual.second)
            .isEqualTo(context.getString(R.string.search_empty_screen_no_audio))
    }

    @Test
    fun `test that when search query is null and user has selected video then No images message is shown to the user`() {
        val actual = underTest(
            isSearchChipEnabled = true,
            category = SearchCategory.VIDEO
        )
        Truth.assertThat(actual.second)
            .isEqualTo(context.getString(R.string.search_empty_screen_no_video))
    }
}