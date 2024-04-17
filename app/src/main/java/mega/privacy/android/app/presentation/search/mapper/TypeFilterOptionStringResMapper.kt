package mega.privacy.android.app.presentation.search.mapper

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

/**
 * Mapper used to map the type filter options to string resources
 */
class TypeFilterOptionStringResMapper @Inject constructor() {

    /**
     * invoke
     *
     * @param typeFilterOption type filter option
     */
    @StringRes
    operator fun invoke(typeFilterOption: TypeFilterOption): Int = when (typeFilterOption) {
        TypeFilterOption.Audio -> R.string.search_dropdown_chip_filter_type_file_type_audio
        TypeFilterOption.Video -> R.string.search_dropdown_chip_filter_type_file_type_video
        TypeFilterOption.Images -> R.string.search_dropdown_chip_filter_type_file_type_images
        TypeFilterOption.Documents -> R.string.search_dropdown_chip_filter_type_file_type_documents
        TypeFilterOption.Folder -> R.string.search_dropdown_chip_filter_type_file_type_folders
        TypeFilterOption.Pdf -> R.string.search_dropdown_chip_filter_type_file_type_pdf
        TypeFilterOption.Presentation -> R.string.search_dropdown_chip_filter_type_file_type_presentations
        TypeFilterOption.Spreadsheet -> R.string.search_dropdown_chip_filter_type_file_type_spreadsheets
        TypeFilterOption.Other -> R.string.search_dropdown_chip_filter_type_file_type_others
    }
}
