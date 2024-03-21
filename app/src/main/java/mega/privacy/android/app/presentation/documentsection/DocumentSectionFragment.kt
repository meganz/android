package mega.privacy.android.app.presentation.documentsection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.fragments.homepage.HomepageSearchable
import mega.privacy.android.app.utils.callManager

/**
 * The fragment for document section
 */
@AndroidEntryPoint
class DocumentSectionFragment : Fragment(), HomepageSearchable {
    private val documentSectionViewModel by viewModels<DocumentSectionViewModel>()

    /**
     * onCreateView
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //TODO Add the view for document section
        return null
    }

    /**
     * onViewCreated
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.collectFlow(
            documentSectionViewModel.uiState.map { it.allDocuments }.distinctUntilChanged()
        ) { list ->
            if (!documentSectionViewModel.uiState.value.searchMode && list.isNotEmpty()) {
                callManager {
                    it.invalidateOptionsMenu()
                }
            }
        }
    }

    /**
     * Should show search menu
     *
     * @return true if should show search menu, false otherwise
     */
    override fun shouldShowSearchMenu(): Boolean = documentSectionViewModel.shouldShowSearchMenu()

    /**
     * Search ready
     */
    override fun searchReady() {
        documentSectionViewModel.searchReady()
    }

    /**
     * Search query
     *
     * @param query query string
     */
    override fun searchQuery(query: String) {
        documentSectionViewModel.searchQuery(query)
    }

    /**
     * Exit search
     */
    override fun exitSearch() {
        documentSectionViewModel.exitSearch()
    }
}