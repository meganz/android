package mega.privacy.android.app.presentation.rubbishbin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.view.NodesView
import javax.inject.Inject

/**
 * Fragment is for Rubbish Bin
 */
class RubbishBinComposeFragment : Fragment() {
    companion object {
        /**
         * Returns the instance of RubbishBinFragment
         */
        @JvmStatic
        fun newInstance() = RubbishBinComposeFragment()
    }

    @Inject
    lateinit var stringUtilWrapper: StringUtilWrapper

    private val viewModel: RubbishBinViewModel by activityViewModels()
    private val sortOrderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * [Boolean] value referenced from [ManagerActivity]
     *
     * If "true", the contents are displayed in a List View-like manner
     * If "false", the contents are displayed in a Grid View-like manner
     */
    private val isList
        get() = (requireActivity() as ManagerActivity).isList

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NodesView(
                    modifier = Modifier.padding(8.dp),
                    nodeUIItems = emptyList(),
                    stringUtilWrapper = stringUtilWrapper,
                    onMenuClick = { },
                    onItemClicked = {
                        viewModel.onItemClicked(nodeUIItem = it)
                    },
                    onLongClick = {
                        viewModel.onLongItemClicked(nodeUIItem = it)
                    },
                    sortOrder = sortOrderViewModel.order.first.name,
                    isListView = isList,
                    onSortOrderClick = { },
                    onChangeViewTypeClick = { },
                )
            }
        }
    }

    /**
     * On back pressed from [ManagerActivity]
     */
    fun onBackPressed() {

    }
}