package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

/**
 * Mapper used to map the type filter options to string
 */
class TypeFilterOptionStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * invoke
     *
     * @param typeFilterOption type filter option
     */
    operator fun invoke(typeFilterOption: TypeFilterOption): String = when (typeFilterOption) {
        TypeFilterOption.Audio -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_audio)
        TypeFilterOption.Video -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_video)
        TypeFilterOption.Images -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_images)
        TypeFilterOption.Documents -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_documents)
        TypeFilterOption.Folder -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_folders)
        TypeFilterOption.Pdf -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_pdf)
        TypeFilterOption.Presentation -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_presentations)
        TypeFilterOption.Spreadsheet -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_spreadsheets)
        TypeFilterOption.Other -> context.getString(R.string.search_dropdown_chip_filter_type_file_type_others)
    }
}