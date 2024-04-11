package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.presentation.search.model.SearchFilter
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.shared.resources.R
import javax.inject.Inject

/**
 * Mapper used to map string names to search categories
 *
 * These strings are shown to the users as filter chips
 */
class SearchFilterMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * invoke
     *
     * @param category search category enum [SearchCategory]
     */
    operator fun invoke(category: SearchCategory) = when (category) {
        SearchCategory.IMAGES -> SearchFilter(
            SearchCategory.IMAGES,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_images)
        )

        SearchCategory.ALL_DOCUMENTS -> SearchFilter(
            SearchCategory.ALL_DOCUMENTS,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_documents)
        )

        SearchCategory.AUDIO -> SearchFilter(
            SearchCategory.AUDIO,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_audio)
        )

        SearchCategory.VIDEO -> SearchFilter(
            SearchCategory.VIDEO,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_video)
        )

        SearchCategory.PDF -> SearchFilter(
            SearchCategory.PDF,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_pdf)
        )

        SearchCategory.PRESENTATION -> SearchFilter(
            SearchCategory.PRESENTATION,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_presentations)
        )

        SearchCategory.SPREADSHEET -> SearchFilter(
            SearchCategory.SPREADSHEET,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_spreadsheets)
        )

        SearchCategory.FOLDER -> SearchFilter(
            SearchCategory.FOLDER,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_folders)
        )

        SearchCategory.OTHER -> SearchFilter(
            SearchCategory.OTHER,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_others)
        )

        SearchCategory.DOCUMENTS -> SearchFilter(
            SearchCategory.DOCUMENTS,
            context.getString(R.string.search_dropdown_chip_filter_type_file_type_documents)
        )

        else -> SearchFilter(
            SearchCategory.ALL,
            ""
        )
    }
}