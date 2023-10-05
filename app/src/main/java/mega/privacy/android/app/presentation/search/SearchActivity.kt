package mega.privacy.android.app.presentation.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.search.model.SearchActivityViewModel
import mega.privacy.android.app.presentation.search.view.SearchComposeView
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.search.SearchType
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Search activity to search Nodes and display
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchActivityViewModel by viewModels()

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    companion object {
        /**
         * Checks if first navigation level
         */
        const val IS_FIRST_LEVEL = "isFirstLevel"

        /**
         * Parent search handle
         */
        const val PARENT_HANDLE = "parentHandle"

        /**
         * Search type
         */
        const val SEARCH_TYPE = "searchType"

        /**
         * Get Search activity Intent
         */
        fun getIntent(
            context: Context,
            searchType: SearchType,
            parentHandle: Long,
            isFirstNavigationLevel: Boolean = false,
        ): Intent = Intent(context, SearchActivity::class.java).apply {
            putExtra(IS_FIRST_LEVEL, isFirstNavigationLevel)
            putExtra(SEARCH_TYPE, searchType)
            putExtra(PARENT_HANDLE, parentHandle)
        }
    }

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                SearchComposeView(
                    state = uiState,
                    sortOrder = getString(
                        SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                            ?: R.string.sortby_name
                    ),
                    onItemClick = {},
                    onLongClick = {},
                    onChangeViewTypeClick = {},
                    onSortOrderClick = {},
                    onMenuClick = {},
                    onDisputeTakeDownClicked = ::navigateToLink,
                    onLinkClicked = ::navigateToLink
                )
            }
        }
    }

    /**
     * Clicked on link
     * @param link
     */
    private fun navigateToLink(link: String) {
        val uriUrl = Uri.parse(link)
        val launchBrowser = Intent(this@SearchActivity, WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(uriUrl)
        startActivity(launchBrowser)
    }
}