package mega.privacy.android.app.contacts.group

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.group.adapter.ContactGroupsAdapter
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.databinding.FragmentContactGroupsBinding
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.MenuUtils.setupSearchView
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

/**
 * Fragment that represents the UI showing the list of contact groups for the current user.
 */
@AndroidEntryPoint
class ContactGroupsFragment : Fragment() {

    private lateinit var binding: FragmentContactGroupsBinding

    private val viewModel by viewModels<ContactGroupsViewModel>()
    private val groupsAdapter by lazy { ContactGroupsAdapter(::onGroupClick) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactGroupsBinding.inflate(inflater, container, false)
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

    private fun setupView() {
        binding.list.adapter = groupsAdapter
        binding.list.setHasFixedSize(true)
        binding.list.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )
        binding.list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val showElevation = recyclerView.canScrollVertically(RecyclerView.NO_POSITION)
                (activity as ContactsActivity?)?.showElevation(showElevation)
            }
        })
        binding.listScroller.setRecyclerView(binding.list)

        binding.btnCreateGroup.setOnClickListener {
            val intent = Intent(
                (activity as ContactsActivity?),
                AddContactActivityLollipop::class.java
            ).apply {
                putExtra(AddContactActivityLollipop.EXTRA_CONTACT_TYPE, Constants.CONTACT_TYPE_MEGA)
                putExtra(AddContactActivityLollipop.EXTRA_ONLY_CREATE_GROUP, true)
            }
            (activity as ContactsActivity?)?.startActivityForResult(
                intent,
                Constants.REQUEST_CREATE_CHAT
            )
        }

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
            .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
            .toSpannedHtmlText()
    }

    private fun setupObservers() {
        viewModel.getGroups().observe(viewLifecycleOwner, ::showGroups)
    }

    /**
     * Show a list of Contact Group View Items on the current RecyclerView.
     *
     * @param items Contact Group View Items to be shown
     */
    private fun showGroups(items: List<ContactGroupItem>) {
        binding.listScroller.isVisible = items.size >= MIN_ITEMS_SCROLLBAR
        binding.viewEmpty.isVisible = items.isNullOrEmpty()
        groupsAdapter.submitList(items)
    }

    /**
     * Callback method called when a Contact Group Item is clicked
     *
     * @param groupId   Group Id that has been clicked
     */
    private fun onGroupClick(groupId: Long) {
        startActivity(Intent(context, GroupChatInfoActivityLollipop::class.java).apply {
            putExtra(Constants.HANDLE, groupId)
        })
    }
}
