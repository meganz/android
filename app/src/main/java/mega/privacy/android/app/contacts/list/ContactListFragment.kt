package mega.privacy.android.app.contacts.list

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.list.adapter.ContactListAdapter
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.FragmentContactListBinding
import mega.privacy.android.app.lollipop.AddContactActivityLollipop
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop
import mega.privacy.android.app.modalbottomsheet.ContactsBottomSheetDialogFragment
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactListFragment : Fragment() {

    private lateinit var binding: FragmentContactListBinding

    private val viewModel by viewModels<ContactListViewModel>()
    private val recentlyAddedAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactMoreInfoClick, false)
    }
    private val contactsAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactMoreInfoClick, true)
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
        inflater.inflate(R.menu.fragment_contact_list, menu)

        menu.findItem(R.id.action_search)?.apply {
            setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    viewModel.setQuery(null)
                    return true
                }

                override fun onMenuItemActionExpand(item: MenuItem?): Boolean = true
            })
            (actionView as SearchView?)?.apply {
                setOnCloseListener {
                    viewModel.setQuery(null)
                    false
                }
                setOnQueryTextListener(object :
                    SearchView.OnQueryTextListener {
                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.setQuery(newText)
                        return true
                    }

                    override fun onQueryTextSubmit(query: String?): Boolean = false
                })
            }
        }
    }

    private fun setupView() {
        val adapterConfig = ConcatAdapter.Config.Builder().setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS).build()
        binding.listContacts.adapter = ConcatAdapter(adapterConfig, recentlyAddedAdapter, contactsAdapter)
        binding.listContacts.setHasFixedSize(true)
        binding.listContacts.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )

        binding.btnRequests.setOnClickListener {
            findNavController().navigate(ContactListFragmentDirections.actionListToRequests())
        }

        binding.btnGroups.setOnClickListener {
            findNavController().navigate(ContactListFragmentDirections.actionListToGroups())
        }

        binding.btnAddContact.setOnClickListener {
            startActivity(Intent(requireContext(), AddContactActivityLollipop::class.java))
        }

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.black)
            .formatColorTag(requireContext(), 'B', R.color.grey_300)
            .toSpannedHtmlText()
    }

    private fun setupObservers() {
        viewModel.getContacts().observe(viewLifecycleOwner, ::showContacts)
        viewModel.getRecentlyAddedContacts().observe(viewLifecycleOwner, ::showRecentlyAddedContacts)
    }

    private fun showContacts(items: List<ContactItem>) {
        binding.txtContacts.isVisible = items.isNotEmpty()
        binding.viewEmpty.isVisible = items.isNullOrEmpty()
        contactsAdapter.submitList(items)
    }

    private fun showRecentlyAddedContacts(items: List<ContactItem>) {
        binding.txtContacts.isVisible = items.isNotEmpty()
        recentlyAddedAdapter.submitList(items)
    }

    private fun onContactClick(userEmail: String) {
        startActivity(Intent(context, ContactInfoActivityLollipop::class.java).apply {
            putExtra(Constants.NAME, userEmail)
        })
    }

    private fun onContactMoreInfoClick(userEmail: String) {
        ContactsBottomSheetDialogFragment
            .newInstance(userEmail)
            .show(childFragmentManager, userEmail)
    }
}
