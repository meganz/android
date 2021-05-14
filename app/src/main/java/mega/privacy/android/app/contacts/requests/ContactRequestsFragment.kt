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
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestListAdapter
import mega.privacy.android.app.databinding.FragmentContactRequestsBinding
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactRequestsFragment : Fragment() {

    companion object {
        private const val STATE_TAB_POSITION = "STATE_TAB"
        private const val TAB_POSITION_OUTGOING = 1
    }

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
                getContactRequests(tab.position == TAB_POSITION_OUTGOING)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        savedInstanceState?.getInt(STATE_TAB_POSITION)?.let { tabPosition ->
            binding.tabs.selectTab(binding.tabs.getTabAt(tabPosition))
            getContactRequests(tabPosition == TAB_POSITION_OUTGOING)
        } ?: run {
            getContactRequests(false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB_POSITION, binding.tabs.selectedTabPosition)
    }

    override fun onDestroyView() {
        binding.tabs.clearOnTabSelectedListeners()
        super.onDestroyView()
    }

    private fun getContactRequests(isOutgoing: Boolean) {
        val contactRequestLiveData = if (isOutgoing) {
            viewModel.getOutgoingRequest()
        } else {
            viewModel.getIncomingRequest()
        }

        contactRequestLiveData.observe(viewLifecycleOwner) { items ->
            showEmptyView(items.isNullOrEmpty(), isOutgoing)
            adapter.submitList(items)
        }
    }

    private fun showEmptyView(show: Boolean, isOutgoing: Boolean = false) {
        if (!show) {
            binding.viewEmpty.isVisible = false
        } else {
            val textRes: Int
            val drawableRes: Int

            if (isOutgoing) {
                textRes = R.string.sent_requests_empty
                drawableRes = R.drawable.ic_zero_data_sent_requests
            } else {
                textRes = R.string.received_requests_empty
                drawableRes = R.drawable.ic_zero_data_received_requests
            }

            binding.viewEmpty.setCompoundDrawablesWithIntrinsicBounds(0, drawableRes, 0, 0)
            binding.viewEmpty.text = getString(textRes)
                .formatColorTag(requireContext(), 'A', R.color.black)
                .formatColorTag(requireContext(), 'B', R.color.grey_300)
                .toSpannedHtmlText()
            binding.viewEmpty.isVisible = true
        }
    }

    private fun onRequestClick(requestHandle: Long) {
        // Do something
    }

    private fun onRequestMoreInfoClick(requestHandle: Long) {
        // Do something
    }
}
