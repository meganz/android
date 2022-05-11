package mega.privacy.android.app.contacts.requests.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.contacts.requests.ContactRequestsViewModel
import mega.privacy.android.app.databinding.BottomSheetContactRequestBinding
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.setImageRequestFromUri

/**
 * Bottom Sheet Dialog that represents the UI for a dialog containing contact request information.
 */
@AndroidEntryPoint
class ContactRequestBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ContactRequestBottomSheetDialogFragment"
        private const val REQUEST_HANDLE = "REQUEST_HANDLE"

        fun newInstance(requestHandle: Long): ContactRequestBottomSheetDialogFragment =
            ContactRequestBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(REQUEST_HANDLE, requestHandle)
                }
            }
    }

    private val viewModel by viewModels<ContactRequestsViewModel>({ requireParentFragment() })
    private val requestHandle by extraNotNull<Long>(REQUEST_HANDLE)

    private lateinit var binding: BottomSheetContactRequestBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetContactRequestBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.itemsLayout
        binding.header.btnMore.isVisible = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.getContactRequest(requestHandle).observe(viewLifecycleOwner) { item ->
            requireNotNull(item) { "Contact request not found" }

            val isOutgoing = item.isOutgoing || item.name.isNullOrBlank()
            binding.header.txtTitle.text = if (isOutgoing) item.email else item.name
            binding.header.txtSubtitle.text = if (isOutgoing) item.createdTime else item.email
            binding.header.imgThumbnail.hierarchy.setPlaceholderImage(item.placeholder)
            binding.header.imgThumbnail.setImageRequestFromUri(item.avatarUri)
            binding.groupReceived.isVisible = !item.isOutgoing
            binding.groupSent.isVisible = item.isOutgoing

            binding.btnAccept.setOnClickListener {
                viewModel.acceptRequest(requestHandle)
                dismiss()
            }
            binding.btnIgnore.setOnClickListener {
                viewModel.ignoreRequest(requestHandle)
                dismiss()
            }
            binding.btnDecline.setOnClickListener {
                viewModel.declineRequest(requestHandle)
                dismiss()
            }
            binding.btnReinvite.setOnClickListener {
                viewModel.reinviteRequest(requestHandle)
                dismiss()
            }
            binding.btnRemove.setOnClickListener {
                viewModel.removeRequest(requestHandle)
                dismiss()
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * Custom show method to avoid showing the same dialog multiple times
     */
    fun show(manager: FragmentManager) {
        if (manager.findFragmentByTag(TAG) == null) {
            super.show(manager, TAG)
        }
    }
}
