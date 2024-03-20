package mega.privacy.android.app.presentation.documentsection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.fragments.homepage.HomepageSearchable

/**
 * The fragment for document section
 */
@AndroidEntryPoint
class DocumentSectionFragment : Fragment(), HomepageSearchable {

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
}