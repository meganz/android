package mega.privacy.android.feature.clouddrive.presentation.search.mapper

import androidx.annotation.StringRes
import mega.privacy.android.domain.entity.search.DateFilterOption
import mega.privacy.android.domain.entity.search.TypeFilterOption
import mega.privacy.android.feature.clouddrive.presentation.search.model.SearchFilterType
import mega.privacy.android.shared.resources.R as sharedR

@get:StringRes
val TypeFilterOption.labelResId: Int
    get() = when (this) {
        TypeFilterOption.Audio -> sharedR.string.search_dropdown_chip_filter_type_file_type_audio
        TypeFilterOption.Video -> sharedR.string.search_dropdown_chip_filter_type_file_type_video
        TypeFilterOption.Images -> sharedR.string.search_dropdown_chip_filter_type_file_type_images
        TypeFilterOption.Documents -> sharedR.string.search_dropdown_chip_filter_type_file_type_documents
        TypeFilterOption.Folder -> sharedR.string.search_dropdown_chip_filter_type_file_type_folders
        TypeFilterOption.Pdf -> sharedR.string.search_dropdown_chip_filter_type_file_type_pdf
        TypeFilterOption.Presentation -> sharedR.string.search_dropdown_chip_filter_type_file_type_presentations
        TypeFilterOption.Spreadsheet -> sharedR.string.search_dropdown_chip_filter_type_file_type_spreadsheets
        TypeFilterOption.Other -> sharedR.string.search_dropdown_chip_filter_type_file_type_others
    }

@get:StringRes
val DateFilterOption.labelResId: Int
    get() = when (this) {
        DateFilterOption.Today -> sharedR.string.search_dropdown_chip_filter_type_date_today
        DateFilterOption.Last7Days -> sharedR.string.search_dropdown_chip_filter_type_date_last_seven_days
        DateFilterOption.Last30Days -> sharedR.string.search_dropdown_chip_filter_type_date_last_thirty_days
        DateFilterOption.ThisYear -> sharedR.string.search_dropdown_chip_filter_type_date_this_year
        DateFilterOption.LastYear -> sharedR.string.search_dropdown_chip_filter_type_date_last_year
        DateFilterOption.Older -> sharedR.string.search_dropdown_chip_filter_type_date_older
    }

@get:StringRes
val SearchFilterType.titleResId: Int
    get() = when (this) {
        SearchFilterType.TYPE -> sharedR.string.search_dropdown_chip_filter_type_file_type
        SearchFilterType.LAST_MODIFIED -> sharedR.string.search_dropdown_chip_filter_type_last_modified
        SearchFilterType.DATE_ADDED -> sharedR.string.search_dropdown_chip_filter_type_date_added
    }
