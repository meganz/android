package mega.privacy.android.app.contacts.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants.*
import mega.privacy.android.app.contacts.list.adapter.ContactActionsListAdapter
import mega.privacy.android.app.contacts.list.adapter.ContactListAdapter
import mega.privacy.android.app.contacts.list.dialog.ContactBottomSheetDialogFragment
import mega.privacy.android.app.databinding.FragmentContactListBinding
import mega.privacy.android.app.lollipop.InviteContactActivity
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactListFragment : Fragment() {

    private lateinit var binding: FragmentContactListBinding

    private val viewModel by viewModels<ContactListViewModel>()
    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                viewModel.retrieveContacts()
            }
        }
    }

    private val actionsAdapter by lazy {
        ContactActionsListAdapter(::onRequestsClick, ::onGroupsClick)
    }
    private val recentlyAddedAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactMoreInfoClick)
    }
    private val contactsAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactMoreInfoClick)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactListBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
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
        val adapterConfig = ConcatAdapter.Config.Builder().setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build()
        binding.list.adapter = ConcatAdapter(adapterConfig, actionsAdapter, recentlyAddedAdapter, contactsAdapter)
        binding.list.setHasFixedSize(true)

        binding.btnAddContact.setOnClickListener {
            startActivity(Intent(requireContext(), InviteContactActivity::class.java))
        }

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
            .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
            .toSpannedHtmlText()
    }

    private fun setupObservers() {
        viewModel.getContactActions().observe(viewLifecycleOwner) { items ->
            actionsAdapter.submitList(items)
        }

        viewModel.getRecentlyAddedContacts().observe(viewLifecycleOwner) { items ->
            recentlyAddedAdapter.submitList(items)
        }

        viewModel.getContactsWithHeaders().observe(viewLifecycleOwner) { items ->
            binding.viewEmpty.isVisible = items.isNullOrEmpty()
            contactsAdapter.submitList(items)
        }
    }

    private fun setupReceivers() {
        val intentFilter = IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE).apply {
            addAction(ACTION_UPDATE_NICKNAME)
            addAction(ACTION_UPDATE_FIRST_NAME)
            addAction(ACTION_UPDATE_LAST_NAME)
            addAction(ACTION_UPDATE_CREDENTIALS)
        }
        activity?.registerReceiver(receiver, intentFilter)
    }

    private fun onContactClick(userEmail: String) {
        ContactUtil.openContactInfoActivity(context, userEmail)
    }

    private fun onContactMoreInfoClick(userHandle: Long) {
        ContactBottomSheetDialogFragment.newInstance(userHandle)
            .show(childFragmentManager, userHandle.toString())
    }

    private fun onRequestsClick() {
        findNavController().navigate(ContactListFragmentDirections.actionListToRequests())
    }

    private fun onGroupsClick() {
        findNavController().navigate(ContactListFragmentDirections.actionListToGroups())
    }
}
