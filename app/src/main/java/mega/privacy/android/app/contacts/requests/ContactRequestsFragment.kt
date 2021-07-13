package mega.privacy.android.app.contacts.requests

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs
import mega.privacy.android.app.databinding.FragmentContactRequestsBinding
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.MenuUtils.setupSearchView

@AndroidEntryPoint
class ContactRequestsFragment : Fragment() {

    companion object {
        const val EXTRA_IS_OUTGOING = "isOutgoing"
        const val STATE_PAGER_POSITION = "STATE_PAGER_POSITION"
    }

    private lateinit var binding: FragmentContactRequestsBinding

    private val isOutgoing by extraNotNull(EXTRA_IS_OUTGOING, false)
    private val viewModel by viewModels<ContactRequestsViewModel>()
    private val toolbarElevation by lazy { resources.getDimension(R.dimen.toolbar_elevation) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactRequestsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_PAGER_POSITION, binding.pager.currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_contact_search, menu)
        menu.findItem(R.id.action_search)?.setupSearchView { query ->
            viewModel.setQuery(query)
        }
    }

    private fun setupView(savedInstanceState: Bundle?) {
        binding.pager.adapter = ContactRequestPageAdapter(this)
        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = if (position == Tabs.INCOMING.ordinal) {
                getString(R.string.tab_received_requests)
            } else {
                getString(R.string.tab_sent_requests)
            }
        }.attach()

        binding.pager.post {
            val defaultPosition = if (isOutgoing) {
                Tabs.OUTGOING.ordinal
            } else {
                Tabs.INCOMING.ordinal
            }

            binding.pager.currentItem = savedInstanceState?.getInt(STATE_PAGER_POSITION, defaultPosition)
                ?: defaultPosition
        }
    }

    fun showElevation(show: Boolean) {
        binding.tabs.elevation = if (show) toolbarElevation else 0F
    }
}
