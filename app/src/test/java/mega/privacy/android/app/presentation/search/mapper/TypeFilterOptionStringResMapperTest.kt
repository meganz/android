package mega.privacy.android.app.presentation.search.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.shared.resources.R
import org.junit.Test

class TypeFilterOptionStringResMapperTest {
    private val typeFilterOptionStringResMapper = TypeFilterOptionStringResMapper()

    @Test
    fun `test that map type filter option to string resource`() {
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Images))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_images)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Documents))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_documents)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Audio))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_audio)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Video))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_video)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Pdf))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_pdf)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Presentation))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_presentations)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Spreadsheet))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_spreadsheets)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Folder))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_folders)
        assertThat(typeFilterOptionStringResMapper(TypeFilterOption.Other))
            .isEqualTo(R.string.search_dropdown_chip_filter_type_file_type_others)
    }
}
