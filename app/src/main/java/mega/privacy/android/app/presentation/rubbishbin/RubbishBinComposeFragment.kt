package mega.privacy.android.app.presentation.rubbishbin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.view.NodesView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * Fragment is for Rubbish Bin
 */
@AndroidEntryPoint
class RubbishBinComposeFragment : Fragment() {
    companion object {
        /**
         * Returns the instance of RubbishBinFragment
         */
        @JvmStatic
        fun newInstance() = RubbishBinComposeFragment()
    }

    /**
     * String formatter for file desc
     */
    @Inject
    lateinit var stringUtilWrapper: StringUtilWrapper

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel: RubbishBinViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.state.collectAsStateWithLifecycle()
                AndroidTheme(isDark = themeMode.isDarkMode()) {
                    NodesView(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        nodeUIItems = uiState.nodeList,
                        stringUtilWrapper = stringUtilWrapper,
                        onMenuClick = { },
                        onItemClicked = viewModel::onItemClicked,
                        onLongClick = viewModel::onLongItemClicked,
                        sortOrder = getString(
                            SortByHeaderViewModel.orderNameMap[uiState.sortOrder]
                                ?: R.string.sortby_name
                        ),
                        isListView = uiState.currentViewType == ViewType.LIST,
                        onSortOrderClick = { showSortByPanel() },
                        onChangeViewTypeClick = viewModel::onChangeViewTypeClicked,
                    )
                }
            }
        }
    }

    /**
     * On back pressed from ManagerActivity
     */
    fun onBackPressed() {

    }

    /**
     * Shows the Sort by panel.
     */
    private fun showSortByPanel() {
        (requireActivity() as ManagerActivity).showNewSortByPanel(Constants.ORDER_CLOUD)
    }
}