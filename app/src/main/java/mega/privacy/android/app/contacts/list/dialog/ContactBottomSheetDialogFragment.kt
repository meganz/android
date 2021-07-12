package mega.privacy.android.app.contacts.list.dialog

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.facebook.imagepipeline.request.ImageRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.list.ContactListViewModel
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.BottomSheetContactDetailBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop.EXTRA_SELECTED_FOLDER
import mega.privacy.android.app.lollipop.controllers.ChatController
import mega.privacy.android.app.lollipop.controllers.ContactController
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import nz.mega.sdk.MegaUser

@AndroidEntryPoint
class ContactBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ContactBottomSheetDialogFragment"
        private const val USER_HANDLE = "USER_HANDLE"
        private const val INVALID_ITEM = -1

        fun newInstance(userHandle: Long): ContactBottomSheetDialogFragment =
            ContactBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(USER_HANDLE, userHandle)
                }
            }
    }

    private val viewModel by viewModels<ContactListViewModel>({ requireParentFragment() })
    private val userHandle by extraNotNull<Long>(USER_HANDLE)

    private lateinit var binding: BottomSheetContactDetailBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetContactDetailBinding.inflate(inflater, container, false)
        contentView = binding.root
        mainLinearLayout = binding.layoutRoot
        items_layout = binding.layoutItems

        binding.header.btnMore.isVisible = false
        binding.header.divider.isVisible = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post { setBottomSheetBehavior(HEIGHT_HEADER_LARGE, true) }

        viewModel.getContact(userHandle).observe(viewLifecycleOwner, ::showContactInfo)
        viewModel.getMegaUser(userHandle).observe(viewLifecycleOwner, ::setupButtons)
    }

    private fun showContactInfo(contact: ContactItem.Data?) {
        requireNotNull(contact) { "Contact not found" }

        binding.header.txtName.text = contact.getTitle()
        binding.header.txtLastSeen.text = contact.lastSeen
        binding.header.txtLastSeen.isVisible = !contact.lastSeen.isNullOrBlank()
        binding.header.imgThumbnail.hierarchy.setPlaceholderImage(contact.placeholder)
        binding.header.imgThumbnail.setImageRequest(ImageRequest.fromUri(contact.avatarUri))
        contact.statusColor?.let { color ->
            binding.header.imgState.setColorFilter(ContextCompat.getColor(requireContext(), color))
        }
    }

    private fun setupButtons(megaUser: MegaUser?) {
        requireNotNull(megaUser) { "MegaUser not found" }

        binding.optionInfo.setOnClickListener {
            ContactUtil.openContactInfoActivity(context, megaUser.email)
            dismiss()
        }

        binding.optionCall.setOnClickListener {
            CallUtil.startNewCall(activity, activity as SnackbarShower, megaUser)
            dismiss()
        }

        binding.optionSendMessage.setOnClickListener {
            viewModel.
            getChatRoomId(megaUser.handle).observe(viewLifecycleOwner) { chatId ->
                val intent = Intent(requireContext(), ChatActivityLollipop::class.java).apply {
                    action = ACTION_CHAT_SHOW_MESSAGES
                    putExtra(CHAT_ID, chatId)
                }
                startActivity(intent)
                dismiss()
            }
        }

        binding.optionSendFile.setOnClickListener {
            val intent = ContactController.getPickFileToSendIntent(requireContext(), listOf(megaUser))
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE)
        }

        binding.optionShareContact.setOnClickListener {
            val intent = ChatController.getSelectChatsToAttachContactIntent(requireContext(), megaUser)
            startActivityForResult(intent, REQUEST_CODE_SELECT_CHAT)
        }

        binding.optionShareFolder.setOnClickListener {
            val intent = ContactController.getPickFolderToShareIntent(requireContext(), listOf(megaUser))
            startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER)
        }

        binding.optionRemove.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(resources.getQuantityString(R.plurals.title_confirmation_remove_contact, 1))
                .setMessage(resources.getQuantityString(R.plurals.confirmation_remove_contact, 1))
                .setNegativeButton(R.string.general_cancel, null)
                .setPositiveButton(R.string.general_remove) { _, _ ->
                    viewModel.removeContact(megaUser)
                    dismiss()
                }
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SELECT_FILE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    viewModel.getMegaUser(userHandle).observe(viewLifecycleOwner) { user ->
                        MegaAttacher(this).handleSelectFileResult(data, user, activity as ContactsActivity)
                        dismiss()
                    }
                }
            }
            REQUEST_CODE_SELECT_FOLDER -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val selectedContacts = data.getStringArrayListExtra(SELECTED_CONTACTS)
                    val folderHandle = data.getLongExtra(EXTRA_SELECTED_FOLDER, 0)
                    val node = megaApi.getNodeByHandle(folderHandle) // TODO Remove `MegaApi` calls here

                    if (node.isFolder) {
                        val permissions = arrayOf(
                            getString(R.string.file_properties_shared_folder_read_only),
                            getString(R.string.file_properties_shared_folder_read_write),
                            getString(R.string.file_properties_shared_folder_full_access)
                        )

                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.file_properties_shared_folder_permissions))
                            .setSingleChoiceItems(permissions, INVALID_ITEM) { dialog: DialogInterface, item: Int ->
                                NodeController(requireContext()).shareFolder(node, selectedContacts, item)
                                dialog.dismiss()
                                dismiss()
                            }
                            .create()
                            .show()
                    }
                }
            }
            REQUEST_CODE_SELECT_CHAT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    MegaAttacher(this).handleActivityResult(requestCode, resultCode, data, activity as ContactsActivity)
                    dismiss()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
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
