package mega.privacy.android.app.presentation.search.mapper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.search.SearchCategory
import mega.privacy.android.domain.entity.search.SearchCategory.*
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
     * @param category search category enum [SearchCategory]
     * If user searches for some text then always "No results" are shown to the user
     * If search query is empty then empty screen is shown based on the filter selected [SearchCategory]
     * If no filter is selected and screen is empty then we show "Cloud drive is empty"
     */
    operator fun invoke(
        category: SearchCategory? = null,
        searchQuery: String? = "",
    ): Pair<Int, String> = when {
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