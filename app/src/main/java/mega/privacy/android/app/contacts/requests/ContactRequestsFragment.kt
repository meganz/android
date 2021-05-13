package mega.privacy.android.app.contacts.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.contacts.list.adapter.ContactListAdapter
import mega.privacy.android.app.databinding.FragmentContactRequestsBinding
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactRequestsFragment : Fragment() {

    private lateinit var binding: FragmentContactRequestsBinding

    private val viewModel by viewModels<ContactRequestsViewModel>()
    private val contactsAdapter: ContactListAdapter by lazy {
        ContactListAdapter(::onContactClick, ::onContactMoreInfoClick, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.listContacts.adapter = contactsAdapter
        binding.listContacts.setHasFixedSize(true)
        binding.listContacts.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val contactRequestLiveData = when (tab.position) {
                    0 -> viewModel.getIncomingRequestContacts()
                    else -> viewModel.getOutgoingRequestContacts()
                }
                contactRequestLiveData.observe(viewLifecycleOwner, ::showContacts)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
        binding.tabs.getTabAt(0)?.select()

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.black)
            .formatColorTag(requireContext(), 'B', R.color.grey_300)
            .toSpannedHtmlText()
    }

    override fun onDestroyView() {
        binding.tabs.clearOnTabSelectedListeners()
        super.onDestroyView()
    }

    private fun showContacts(items: List<ContactItem>) {
        binding.viewEmpty.isVisible = items.isNullOrEmpty()
        contactsAdapter.submitList(items)
    }

    private fun onContactClick(userHandle: Long) {
        // Do something
    }

    private fun onContactMoreInfoClick(userHandle: Long) {
        // Do something
    }
}
