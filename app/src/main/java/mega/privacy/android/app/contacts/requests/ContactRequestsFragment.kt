package mega.privacy.android.app.contacts.requests

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.constants.BroadcastConstants
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestListAdapter
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.requests.dialog.ContactRequestBottomSheetDialogFragment
import mega.privacy.android.app.databinding.FragmentContactRequestsBinding
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactRequestsFragment : Fragment() {

    companion object {
        const val EXTRA_IS_OUTGOING = "isOutgoing"
        private const val STATE_TAB_POSITION = "STATE_TAB"
        private const val TAB_POSITION_INCOMING = 0
        private const val TAB_POSITION_OUTGOING = 1
    }

    private lateinit var binding: FragmentContactRequestsBinding

    private val isOutgoing by extraNotNull(EXTRA_IS_OUTGOING, false)
    private val viewModel by viewModels<ContactRequestsViewModel>()
    private val adapter by lazy { ContactRequestListAdapter(::onRequestClick) }
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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView(savedInstanceState)
        setupReceivers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB_POSITION, binding.tabs.selectedTabPosition)
    }

    override fun onDestroyView() {
        binding.tabs.clearOnTabSelectedListeners()
        activity?.unregisterReceiver(receiver)
        super.onDestroyView()
    }

    private fun setupView(savedInstanceState: Bundle?) {
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)
        binding.list.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )

        savedInstanceState?.getInt(STATE_TAB_POSITION)?.let { tabPosition ->
            getContactRequests(tabPosition == TAB_POSITION_OUTGOING)
        } ?: run {
            getContactRequests(isOutgoing)
        }

        binding.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                getContactRequests(tab.position == TAB_POSITION_OUTGOING)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupReceivers() {
        val intentFilter = IntentFilter(BroadcastConstants.BROADCAST_ACTION_REQUEST_UPDATE)
        activity?.registerReceiver(receiver, intentFilter)
    }

    private fun getContactRequests(isOutgoing: Boolean) {
        val requestLiveData: LiveData<List<ContactRequestItem>>
        val newTabPosition: Int
        if (isOutgoing) {
            newTabPosition = TAB_POSITION_OUTGOING
            requestLiveData = viewModel.getOutgoingRequest()
        } else {
            newTabPosition = TAB_POSITION_INCOMING
            requestLiveData = viewModel.getIncomingRequest()
        }

        if (binding.tabs.selectedTabPosition != newTabPosition) {
            binding.tabs.selectTab(binding.tabs.getTabAt(newTabPosition))
        }

        requestLiveData.observe(viewLifecycleOwner) { items ->
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
                .formatColorTag(requireContext(), 'A', R.color.grey_900_grey_100)
                .formatColorTag(requireContext(), 'B', R.color.grey_300_grey_600)
                .toSpannedHtmlText()
            binding.viewEmpty.isVisible = true
        }
    }

    private fun onRequestClick(requestHandle: Long) {
        ContactRequestBottomSheetDialogFragment.newInstance(requestHandle)
            .show(childFragmentManager, requestHandle.toString())
    }
}
