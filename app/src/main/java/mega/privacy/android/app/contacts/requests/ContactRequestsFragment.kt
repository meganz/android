package mega.privacy.android.app.contacts.requests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestListAdapter
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.databinding.FragmentContactRequestsBinding
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactRequestsFragment : Fragment() {

    private lateinit var binding: FragmentContactRequestsBinding

    private val viewModel by viewModels<ContactRequestsViewModel>()
    private val adapter by lazy {
        ContactRequestListAdapter(::onRequestClick, ::onRequestMoreInfoClick)
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
        binding.listContacts.adapter = adapter
        binding.listContacts.setHasFixedSize(true)
        binding.listContacts.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                getContactRequests(tab.position == 1)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.viewEmpty.text = binding.viewEmpty.text.toString()
            .formatColorTag(requireContext(), 'A', R.color.black)
            .formatColorTag(requireContext(), 'B', R.color.grey_300)
            .toSpannedHtmlText()

        getContactRequests()
    }

    private fun getContactRequests(isOutgoing: Boolean = false) {
        val contactRequestLiveData = if (isOutgoing) {
            viewModel.getOutgoingRequest()
        } else {
            viewModel.getIncomingRequest()
        }
        contactRequestLiveData.observe(viewLifecycleOwner, ::showRequests)
    }

    override fun onDestroyView() {
        binding.tabs.clearOnTabSelectedListeners()
        super.onDestroyView()
    }

    private fun showRequests(items: List<ContactRequestItem>) {
        adapter.submitList(items)
    }

    private fun onRequestClick(requestHandle: Long) {
        // Do something
    }

    private fun onRequestMoreInfoClick(requestHandle: Long) {
        // Do something
    }
}
