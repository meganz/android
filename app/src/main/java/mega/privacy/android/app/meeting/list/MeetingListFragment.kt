package mega.privacy.android.app.meeting.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.FragmentMeetingListBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.Util

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
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMeetingListBinding.inflate(inflater, container, false)
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
            viewModel.setSearchQuery(query)
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

                if (recyclerView.canScrollVertically(RecyclerView.NO_POSITION)) {
                    (activity as? ManagerActivity)?.changeAppBarElevation(Util.isDarkMode(context))
                } else {
                    (activity as? ManagerActivity)?.changeAppBarElevation(false)
                }
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
    }

    private fun onMeetingClick(chatId: Long) {
        TODO()
    }

    private fun onContactMoreClick(userHandle: Long) {
        TODO()
    }
}
