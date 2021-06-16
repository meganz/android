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
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.contacts.requests.ContactRequestsFragment.Companion.EXTRA_IS_OUTGOING
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestListAdapter
import mega.privacy.android.app.contacts.requests.dialog.ContactRequestBottomSheetDialogFragment
import mega.privacy.android.app.databinding.PageContactRequestsBinding
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.StringUtils.formatColorTag
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText

@AndroidEntryPoint
class ContactRequestsPageFragment : Fragment() {

    companion object {
        fun newInstance(isOutgoing: Boolean): ContactRequestsPageFragment =
            ContactRequestsPageFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(EXTRA_IS_OUTGOING, isOutgoing)
                }
            }
    }

    private lateinit var binding: PageContactRequestsBinding

    private val isOutgoing by extraNotNull(EXTRA_IS_OUTGOING, false)
    private val viewModel by viewModels<ContactRequestsViewModel>({ requireParentFragment() })
    private val adapter by lazy { ContactRequestListAdapter(::onRequestClick) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PageContactRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    private fun setupObservers() {
        val requestLiveData = if (isOutgoing) {
            viewModel.getOutgoingRequest()
        } else {
            viewModel.getIncomingRequest()
        }

        requestLiveData.observe(viewLifecycleOwner) { items ->
            showEmptyView(items.isNullOrEmpty(), isOutgoing)
            adapter.submitList(items)
        }
    }

    private fun setupView() {
        binding.list.adapter = adapter
        binding.list.setHasFixedSize(true)
        binding.list.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                setDrawable(ResourcesCompat.getDrawable(resources, R.drawable.contact_list_divider, null)!!)
            }
        )
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
