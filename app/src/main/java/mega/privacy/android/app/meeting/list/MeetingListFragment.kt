package mega.privacy.android.app.meeting.list

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.list.dialog.ContactBottomSheetDialogFragment
import mega.privacy.android.app.databinding.FragmentMeetingListBinding
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class MeetingListFragment : Fragment() {

    private lateinit var binding: FragmentMeetingListBinding

    private val viewModel by viewModels<MeetingListViewModel>()
//    private val contactsAdapter by lazy {
//        ContactListAdapter(::onContactClick, ::onContactInfoClick, ::onContactMoreClick)
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMeetingListBinding.inflate(inflater, container, false)
//        setHasOptionsMenu(true)
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

    private fun setupView() {
//        binding.list.adapter = ConcatAdapter(adapterConfig, actionsAdapter, recentlyAddedAdapter, contactsAdapter)
        binding.list.setHasFixedSize(true)
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val showElevation = recyclerView.canScrollVertically(RecyclerView.NO_POSITION)
                (activity as ContactsActivity?)?.showElevation(showElevation)
            }
        })
        binding.listScroller.setRecyclerView(binding.list)

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
            .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
            .toSpannedHtmlText()
    }

    private fun setupObservers() {
//        viewModel.getContactActions().observe(viewLifecycleOwner) { items ->
//            actionsAdapter.submitList(items)
//        }
//
//        viewModel.getRecentlyAddedContacts().observe(viewLifecycleOwner) { items ->
//            recentlyAddedAdapter.submitList(items)
//        }
//
//        viewModel.getContactsWithHeaders().observe(viewLifecycleOwner) { items ->
//            binding.listScroller.isVisible = items.size >= MIN_ITEMS_SCROLLBAR
//            binding.viewEmpty.isVisible = items.isNullOrEmpty()
//            contactsAdapter.submitList(items)
//        }
    }

    private fun onContactClick(userHandle: Long) {
//        viewModel.getChatRoomId(userHandle).observe(viewLifecycleOwner) { chatId ->
//            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
//                action = Constants.ACTION_CHAT_SHOW_MESSAGES
//                putExtra(Constants.CHAT_ID, chatId)
//            }
//            startActivity(intent)
//        }
    }

    private fun onContactInfoClick(userEmail: String) {
        ContactUtil.openContactInfoActivity(context, userEmail)
    }

    private fun onContactMoreClick(userHandle: Long) {
        ContactBottomSheetDialogFragment.newInstance(userHandle).show(childFragmentManager)
    }
}
