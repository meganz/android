package mega.privacy.android.app.contacts.requests

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs
import mega.privacy.android.app.databinding.FragmentContactRequestsBinding
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.MenuUtils.setupSearchView

@AndroidEntryPoint
class ContactRequestsFragment : Fragment() {

    companion object {
        const val EXTRA_IS_OUTGOING = "isOutgoing"
    }

    private lateinit var binding: FragmentContactRequestsBinding

    private val isOutgoing by extraNotNull(EXTRA_IS_OUTGOING, false)
    private val viewModel by viewModels<ContactRequestsViewModel>()
    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                viewModel.updateRequests()
            }
        }
    }

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
        setupView()
        setupReceivers()
    }

    override fun onDestroyView() {
        activity?.unregisterReceiver(receiver)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_contact_search, menu)
        menu.findItem(R.id.action_search)?.setupSearchView { query ->
            viewModel.setQuery(query)
        }
    }

    private fun setupView() {
        binding.pager.adapter = ContactRequestPageAdapter(this)

        TabLayoutMediator(binding.tabs, binding.pager) { tab, position ->
            tab.text = if (position == Tabs.INCOMING.ordinal) {
                getString(R.string.tab_received_requests)
            } else {
                getString(R.string.tab_sent_requests)
            }
        }.attach()

        binding.pager.post {
            binding.pager.currentItem = if (isOutgoing) {
                Tabs.OUTGOING.ordinal
            } else {
                Tabs.INCOMING.ordinal
            }
        }
    }

    private fun setupReceivers() {
        val intentFilter = IntentFilter(BroadcastConstants.BROADCAST_ACTION_REQUEST_UPDATE)
        activity?.registerReceiver(receiver, intentFilter)
    }
}
