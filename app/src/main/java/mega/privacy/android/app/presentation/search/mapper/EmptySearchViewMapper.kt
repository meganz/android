package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchCategory.*
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject

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
    ): Pair<Int, String> {
        return if (isSearchChipEnabled.not()) {
            if (searchParentHandle == INVALID_HANDLE) {
                val res =
                    if (context.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        R.drawable.ic_zero_landscape_empty_folder
                    } else {
                        R.drawable.ic_zero_portrait_empty_folder
                    }
                Pair(res, context.getString(R.string.no_results_found))
            } else if (rootNodeHandle == searchParentHandle) {
                val res =
                    if (context.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        R.drawable.cloud_empty_landscape
                    } else {
                        R.drawable.ic_empty_cloud_drive
                    }
                Pair(res, context.getString(R.string.context_empty_cloud_drive))
            } else {
                val res =
                    if (context.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        R.drawable.ic_zero_landscape_empty_folder
                    } else {
                        R.drawable.ic_zero_portrait_empty_folder
                    }
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

                category == DOCUMENTS -> Pair(
                    R.drawable.ic_homepage_empty_document,
                    context.getString(R.string.search_empty_screen_no_documents)
                )

                category == AUDIO -> Pair(
                    R.drawable.ic_homepage_empty_audio,
                    context.getString(R.string.search_empty_screen_no_audio)
                )

                category == VIDEO -> Pair(
                    R.drawable.ic_homepage_empty_video,
                    context.getString(R.string.search_empty_screen_no_video)
                )

                else -> Pair(
                    R.drawable.cloud_empty_landscape,
                    context.getString(R.string.cloud_drive_empty_screen_message)
                )
            }
        }
    }
}