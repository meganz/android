package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.search.TypeFilterOption
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TypeFilterOptionStringMapperTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val typeFilterOptionStringMapper = TypeFilterOptionStringMapper(context)

    @Test
    fun `test that map type filter option to string`() {
        assertThat(typeFilterOptionStringMapper(TypeFilterOption.Images))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_file_type_images))
        assertThat(typeFilterOptionStringMapper(TypeFilterOption.Documents))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_file_type_documents))
        assertThat(typeFilterOptionStringMapper(TypeFilterOption.Audio))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_file_type_audio))
        assertThat(typeFilterOptionStringMapper(TypeFilterOption.Video))
            .isEqualTo(context.getString(R.string.search_dropdown_chip_filter_type_file_type_video))
    }
}