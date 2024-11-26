package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchCategory.ALL
import mega.privacy.android.domain.entity.search.SearchCategory.ALL_DOCUMENTS
import mega.privacy.android.domain.entity.search.SearchCategory.AUDIO
import mega.privacy.android.domain.entity.search.SearchCategory.DOCUMENTS
import mega.privacy.android.domain.entity.search.SearchCategory.FOLDER
import mega.privacy.android.domain.entity.search.SearchCategory.IMAGES
import mega.privacy.android.domain.entity.search.SearchCategory.OTHER
import mega.privacy.android.domain.entity.search.SearchCategory.PDF
import mega.privacy.android.domain.entity.search.SearchCategory.PRESENTATION
import mega.privacy.android.domain.entity.search.SearchCategory.SPREADSHEET
import mega.privacy.android.domain.entity.search.SearchCategory.VIDEO
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject
import mega.privacy.android.icon.pack.R as iconPackR

/**
 * Empty search view mapper
 */
class EmptySearchViewMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * invoke
     *
     * @param isSearchChipEnabled if filter chip enabled
     * @param category search category enum [SearchCategory]
     * @param searchParentHandle searchParentHandle
     * @param rootNodeHandle root Node handle
     * @param isDateFilterApplied if there is a date filter applied
     * If user searches for some text then always "No results" are shown to the user
     * If search query is empty then empty screen is shown based on the filter selected [SearchCategory]
     * If no filter is selected and screen is empty then we show "Cloud drive is empty"
     */
    operator fun invoke(
        isSearchChipEnabled: Boolean,
        category: SearchCategory? = null,
        searchQuery: String? = "",
        searchParentHandle: Long = -1,
        rootNodeHandle: Long = -1,
        isDateFilterApplied: Boolean = false,
    ): Pair<Int, String> {
        return if (isSearchChipEnabled.not()) {
            if (searchParentHandle == INVALID_HANDLE) {
                val res = iconPackR.drawable.ic_empty_folder_glass
                Pair(res, context.getString(R.string.no_results_found))
            } else if (rootNodeHandle == searchParentHandle) {
                val res = iconPackR.drawable.ic_empty_cloud_glass
                Pair(res, context.getString(R.string.context_empty_cloud_drive))
            } else {
                val res = iconPackR.drawable.ic_empty_folder_glass
                Pair(res, context.getString(R.string.file_browser_empty_folder_new))
            }
        } else {
            when {
                !searchQuery.isNullOrEmpty() -> {
                    Pair(
                        R.drawable.ic_empty_search,
                        context.getString(R.string.search_empty_screen_no_results)
                    )
                }

                category == IMAGES -> Pair(
                    R.drawable.ic_no_images,
                    context.getString(R.string.search_empty_screen_no_images)
                )

                category == ALL_DOCUMENTS -> Pair(
                    iconPackR.drawable.ic_files_glass,
                    context.getString(R.string.search_empty_screen_no_documents)
                )

                category == AUDIO -> Pair(
                    iconPackR.drawable.ic_audio_glass,
                    context.getString(R.string.search_empty_screen_no_audio)
                )

                category == VIDEO -> Pair(
                    iconPackR.drawable.ic_video_glass,
                    context.getString(R.string.search_empty_screen_no_video)
                )

                category == PDF
                        || category == PRESENTATION
                        || category == SPREADSHEET
                        || category == FOLDER
                        || category == OTHER
                        || category == DOCUMENTS -> Pair(
                    R.drawable.ic_empty_search,
                    context.getString(R.string.search_empty_screen_no_results)
                )

                category == ALL && isDateFilterApplied -> Pair(
                    R.drawable.ic_empty_search,
                    context.getString(R.string.search_empty_screen_no_results)
                )

                else -> Pair(
                    iconPackR.drawable.ic_empty_cloud_glass,
                    context.getString(R.string.cloud_drive_empty_screen_message)
                )
            }
        }
    }
}