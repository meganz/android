package mega.privacy.android.app.contacts.list

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.list.adapter.ContactActionsListAdapter
import mega.privacy.android.app.contacts.list.adapter.ContactListAdapter
import mega.privacy.android.app.contacts.list.dialog.ContactBottomSheetDialogFragment
import mega.privacy.android.app.databinding.FragmentContactListBinding
import mega.privacy.android.app.main.InviteContactActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

/**
 * Fragment that represents the UI showing the list of contacts for the current user.
 */
@AndroidEntryPoint
class ContactListFragment : Fragment() {

    private lateinit var binding: FragmentContactListBinding

    private val viewModel by viewModels<ContactListViewModel>()
    private val actionsAdapter by lazy {
        ContactActionsListAdapter(::onRequestsClick, ::onGroupsClick)
    }
    private val recentlyAddedAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactInfoClick, ::onContactMoreClick)
    }
    private val contactsAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactInfoClick, ::onContactMoreClick)
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
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_contact_search, menu)
        menu.findItem(R.id.action_search)?.setupSearchView { query ->
            viewModel.setQuery(query)
        }
    }

    override fun onDestroyView() {
        binding.list.clearOnScrollListeners()
        super.onDestroyView()
    }

    /**
     * Start call
     */
    fun startCall() {
        viewModel.startCall()
    }

    private fun setupView() {
        val adapterConfig = ConcatAdapter.Config.Builder().setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build()
        binding.list.adapter = ConcatAdapter(adapterConfig, actionsAdapter, recentlyAddedAdapter, contactsAdapter)
        binding.list.setHasFixedSize(true)
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val showElevation = recyclerView.canScrollVertically(RecyclerView.NO_POSITION)
                (activity as ContactsActivity?)?.showElevation(showElevation)
            }
        })
        binding.listScroller.setRecyclerView(binding.list)

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
            binding.listScroller.isVisible = items.size >= MIN_ITEMS_SCROLLBAR
            binding.viewEmpty.isVisible = items.isNullOrEmpty()
            contactsAdapter.submitList(items)
        }
    }

    private fun onContactClick(userHandle: Long) {
        viewModel.getChatRoomId(userHandle).observe(viewLifecycleOwner) { chatId ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                action = Constants.ACTION_CHAT_SHOW_MESSAGES
                putExtra(Constants.CHAT_ID, chatId)
            }
            startActivity(intent)
        }
    }

    private fun onContactInfoClick(userEmail: String) {
        ContactUtil.openContactInfoActivity(context, userEmail)
    }

    private fun onContactMoreClick(userHandle: Long) {
        ContactBottomSheetDialogFragment.newInstance(userHandle).show(childFragmentManager)
    }

    private fun onRequestsClick() {
        findNavController().navigate(ContactListFragmentDirections.actionListToRequests())
    }

    private fun onGroupsClick() {
        findNavController().navigate(ContactListFragmentDirections.actionListToGroups())
    }
}
