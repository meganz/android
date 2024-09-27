package mega.privacy.android.app.contacts.list.dialog

import android.Manifest
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SelectChatsToAttachActivityContract
import mega.privacy.android.app.activities.contract.SelectFileToShareActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToShareActivityContract
import mega.privacy.android.app.contacts.list.ContactListViewModel
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.BottomSheetContactDetailBinding
import mega.privacy.android.app.main.FileExplorerActivity.Companion.EXTRA_SELECTED_FOLDER
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.transfers.attach.NodeAttachmentViewModel
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.Constants.SELECTED_CHATS
import mega.privacy.android.app.utils.Constants.SELECTED_CONTACTS
import mega.privacy.android.app.utils.Constants.SELECTED_USERS
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.ContactAvatar
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.navigation.MegaNavigator
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import javax.inject.Inject

/**
 * Bottom Sheet Dialog that represents the UI for a dialog containing contact information.
 */
@AndroidEntryPoint
class ContactBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val TAG = "ContactBottomSheetDialogFragment"
        private const val USER_HANDLE = "USER_HANDLE"
        private const val INVALID_ITEM = -1
        private const val STATE_SHOW_REMOVE_DIALOG = "STATE_SHOW_REMOVE_DIALOG"
        private const val STATE_NODE_FOLDER = "STATE_NODE_FOLDER"
        private const val STATE_NODE_CONTACTS = "STATE_NODE_CONTACTS"

        /**
         * Main method to create a ContactBottomSheetDialogFragment.
         *
         * @param userHandle    User to show information about
         * @return              ContactBottomSheetDialogFragment to be shown
         */
        fun newInstance(userHandle: Long): ContactBottomSheetDialogFragment =
            ContactBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(USER_HANDLE, userHandle)
                }
            }
    }

    @Inject
    lateinit var passcodeManagement: PasscodeManagement

    @Inject
    lateinit var navigator: MegaNavigator

    /**
     * Send message button click listener
     *
     * @param handle mega user's handle from
     */
    var optionSendMessageClick: ((handle: Long) -> Unit)? = null

    private val viewModel by viewModels<ContactListViewModel>({ requireParentFragment() })
    private val userHandle by lazy {
        arguments?.getLong(USER_HANDLE, INVALID_HANDLE) ?: INVALID_HANDLE
    }

    private val nodeAttachmentViewModel by viewModels<NodeAttachmentViewModel>(ownerProducer = { requireParentFragment() })
    private var removeContactDialog: AlertDialog? = null
    private var nodePermissionsDialog: AlertDialog? = null
    private var selectedContacts: ArrayList<String>? = null
    private var folderHandle: Long? = null

    private lateinit var binding: BottomSheetContactDetailBinding
    private lateinit var selectFileLauncher: ActivityResultLauncher<String>
    private lateinit var selectFolderLauncher: ActivityResultLauncher<String>
    private lateinit var selectChatLauncher: ActivityResultLauncher<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectFileLauncher =
            registerForActivityResult(SelectFileToShareActivityContract()) { result ->
                if (result != null) {
                    viewModel.getContactEmail(userHandle).observe(viewLifecycleOwner) { email ->
                        val nodes = result.getLongArrayExtra(NODE_HANDLES)
                        if (nodes != null && nodes.isNotEmpty() && !email.isNullOrEmpty()) {
                            nodeAttachmentViewModel.attachNodesToChatByEmail(
                                nodeIds = nodes.map { handle -> NodeId(handle) },
                                email = email
                            )
                        }
                        dismiss()
                    }
                }
            }

        selectFolderLauncher =
            registerForActivityResult(SelectFolderToShareActivityContract()) { result ->
                if (result != null) {
                    selectedContacts = result.getStringArrayListExtra(SELECTED_CONTACTS)
                    folderHandle = result.getLongExtra(EXTRA_SELECTED_FOLDER, 0)
                    showNodePermissionsDialog()
                }
            }

        selectChatLauncher =
            registerForActivityResult(SelectChatsToAttachActivityContract()) { result ->
                if (result != null) {
                    viewModel.getContactEmail(userHandle).observe(viewLifecycleOwner) { email ->
                        if (!email.isNullOrEmpty()) {
                            val chatIds = result.getLongArrayExtra(SELECTED_CHATS) ?: longArrayOf()
                            val userHandles =
                                result.getLongArrayExtra(SELECTED_USERS) ?: longArrayOf()
                            nodeAttachmentViewModel.attachContactToChat(
                                email = email,
                                chatIds = chatIds,
                                userHandles = userHandles
                            )
                        }
                        dismiss()
                    }
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetContactDetailBinding.inflate(inflater, container, false)
        contentView = binding.root
        itemsLayout = binding.layoutItems

        binding.header.btnMore.isVisible = false
        binding.header.divider.isVisible = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.getContact(userHandle).observe(viewLifecycleOwner, ::showContactInfo)
        viewModel.getContactEmail(userHandle).observe(viewLifecycleOwner) { contactEmail ->
            contactEmail?.let {
                setupButtons(it, userHandle)

                if (savedInstanceState?.getBoolean(STATE_SHOW_REMOVE_DIALOG) == true) {
                    showRemoveContactDialog(it)
                }
            }
            if (savedInstanceState?.containsKey(STATE_NODE_FOLDER) == true) {
                folderHandle = savedInstanceState.getLong(STATE_NODE_FOLDER)
                selectedContacts = savedInstanceState.getStringArrayList(STATE_NODE_CONTACTS)
                showNodePermissionsDialog()
            }
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SHOW_REMOVE_DIALOG, removeContactDialog?.isShowing ?: false)
        if (nodePermissionsDialog?.isShowing == true && folderHandle != null) {
            outState.putLong(STATE_NODE_FOLDER, folderHandle!!)
            outState.putStringArrayList(STATE_NODE_CONTACTS, selectedContacts)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        removeContactDialog?.dismiss()
        nodePermissionsDialog?.dismiss()
        super.onDestroyView()
    }

    /**
     * Show contact information on the UI.
     *
     * @param contact   Contact to be shown
     */
    private fun showContactInfo(contact: ContactItem.Data?) {
        requireNotNull(contact) { "Contact not found" }

        binding.header.txtName.text = contact.getTitle()
        binding.header.txtLastSeen.text = contact.lastSeen
        binding.header.txtLastSeen.isVisible = !contact.lastSeen.isNullOrBlank()
        binding.header.imgThumbnail.load(
            data = ContactAvatar(id = UserId(contact.handle))
        ) {
            transformations(CircleCropTransformation())
            placeholder(contact.placeholder)
        }

        contact.statusColor?.let { color ->
            binding.header.imgState.setColorFilter(ContextCompat.getColor(requireContext(), color))
        }
        binding.header.verifiedIcon.isVisible = contact.isVerified
    }

    /**
     * Setup option buttons according to the current MegaUser.
     *
     * @param megaUser  MegaUser to be shown
     */
    private fun setupButtons(contactEmail: String, contactHandle: Long) {
        binding.optionInfo.setOnClickListener {
            ContactUtil.openContactInfoActivity(context, contactEmail)
            dismiss()
        }

        binding.optionCall.setOnClickListener {
            MegaApplication.userWaitingForCall = contactHandle
            if (CallUtil.canCallBeStartedFromContactOption(requireActivity(), passcodeManagement)) {
                val audio = PermissionUtils.hasPermissions(
                    requireContext(),
                    Manifest.permission.RECORD_AUDIO
                )
                viewModel.onCallTap(video = false, audio = audio)
            }
            dismiss()
        }

        binding.optionSendMessage.setOnClickListener {
            optionSendMessageClick?.invoke(contactHandle)
        }

        binding.optionSendFile.setOnClickListener {
            selectFileLauncher.launch(contactEmail)
        }

        binding.optionShareContact.setOnClickListener {
            selectChatLauncher.launch(contactHandle)
        }

        binding.optionShareFolder.setOnClickListener {
            selectFolderLauncher.launch(contactEmail)
        }

        binding.optionRemove.setOnClickListener { showRemoveContactDialog(contactEmail) }
    }

    /**
     * Show remove contact dialog to allow contact removal.
     *
     * @param megaUser  MegaUser to be removed
     */
    private fun showRemoveContactDialog(contactEmail: String) {
        if (removeContactDialog?.isShowing == true) removeContactDialog?.dismiss()

        removeContactDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getQuantityString(R.plurals.title_confirmation_remove_contact, 1))
            .setMessage(resources.getQuantityString(R.plurals.confirmation_remove_contact, 1))
            .setNegativeButton(R.string.general_cancel, null)
            .setPositiveButton(R.string.general_remove) { _, _ ->
                viewModel.removeContact(contactEmail)
                dismiss()
            }
            .show()
    }

    /**
     * Show node permission dialog to ask for Node permissions.
     */
    private fun showNodePermissionsDialog() {
        if (nodePermissionsDialog?.isShowing == true) nodePermissionsDialog?.dismiss()

        val node = megaApi.getNodeByHandle(folderHandle!!)
        if (node?.isFolder == true) {
            val permissions = arrayOf(
                getString(R.string.file_properties_shared_folder_read_only),
                getString(R.string.file_properties_shared_folder_read_write),
                getString(R.string.file_properties_shared_folder_full_access)
            )

            nodePermissionsDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.file_properties_shared_folder_permissions))
                .setSingleChoiceItems(
                    permissions,
                    INVALID_ITEM
                ) { dialog: DialogInterface, item: Int ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.initShareKey(node)
                        NodeController(requireContext()).shareFolder(node, selectedContacts, item)
                        folderHandle = null
                        selectedContacts = null

                        dialog.dismiss()
                        dismiss()
                    }
                }
                .show()
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
