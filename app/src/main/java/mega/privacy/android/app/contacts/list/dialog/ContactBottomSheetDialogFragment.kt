package mega.privacy.android.app.contacts.list.dialog

import android.app.Activity.RESULT_OK
import android.content.DialogInterface
import android.content.Intent
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.activities.contract.SelectChatsToAttachActivityContract
import mega.privacy.android.app.activities.contract.SelectFileToShareActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToShareActivityContract
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.list.ContactListViewModel
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.databinding.BottomSheetContactDetailBinding
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.FileExplorerActivity.EXTRA_SELECTED_FOLDER
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.ExtraUtils.extraNotNull
import mega.privacy.android.app.utils.setImageRequestFromUri
import nz.mega.sdk.MegaUser
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

    private val viewModel by viewModels<ContactListViewModel>({ requireParentFragment() })
    private val userHandle by extraNotNull<Long>(USER_HANDLE)
    private val nodeAttacher: MegaAttacher by lazy { MegaAttacher(this) }
    private var removeContactDialog: AlertDialog? = null
    private var nodePermissionsDialog: AlertDialog? = null
    private var selectedContacts: ArrayList<String>? = null
    private var folderHandle: Long? = null

    private lateinit var binding: BottomSheetContactDetailBinding
    private lateinit var selectFileLauncher: ActivityResultLauncher<List<MegaUser>>
    private lateinit var selectFolderLauncher: ActivityResultLauncher<List<MegaUser>>
    private lateinit var selectChatLauncher: ActivityResultLauncher<MegaUser>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        selectFileLauncher = registerForActivityResult(SelectFileToShareActivityContract()) { result ->
            if (result != null) {
                viewModel.getMegaUser(userHandle).observe(viewLifecycleOwner) { user ->
                    nodeAttacher.handleSelectFileResult(result, user, activity as ContactsActivity)
                    dismiss()
                }
            }
        }

        selectFolderLauncher = registerForActivityResult(SelectFolderToShareActivityContract()) { result ->
            if (result != null) {
                selectedContacts = result.getStringArrayListExtra(SELECTED_CONTACTS)
                folderHandle = result.getLongExtra(EXTRA_SELECTED_FOLDER, 0)
                showNodePermissionsDialog()
            }
        }

        selectChatLauncher = registerForActivityResult(SelectChatsToAttachActivityContract()) { result ->
            if (result != null) {
                nodeAttacher.handleActivityResult(REQUEST_CODE_SELECT_CHAT, RESULT_OK, result, activity as ContactsActivity)
                dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
        viewModel.getMegaUser(userHandle).observe(viewLifecycleOwner) { megaUser ->
            setupButtons(megaUser)

            if (savedInstanceState?.getBoolean(STATE_SHOW_REMOVE_DIALOG) == true) {
                showRemoveContactDialog(megaUser)
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
        binding.header.imgThumbnail.hierarchy.setPlaceholderImage(contact.placeholder)
        binding.header.imgThumbnail.setImageRequestFromUri(contact.avatarUri)
        contact.statusColor?.let { color ->
            binding.header.imgState.setColorFilter(ContextCompat.getColor(requireContext(), color))
        }
    }

    /**
     * Setup option buttons according to the current MegaUser.
     *
     * @param megaUser  MegaUser to be shown
     */
    private fun setupButtons(megaUser: MegaUser?) {
        requireNotNull(megaUser) { "MegaUser not found" }

        binding.optionInfo.setOnClickListener {
            ContactUtil.openContactInfoActivity(context, megaUser.email)
            dismiss()
        }

        binding.optionCall.setOnClickListener {
            if (CallUtil.canCallBeStartedFromContactOption(requireActivity(), passcodeManagement)) {
                CallUtil.startNewCall(
                    activity,
                    activity as SnackbarShower,
                    megaUser,
                    passcodeManagement
                )
                dismiss()
            }
        }

        binding.optionSendMessage.setOnClickListener {
            viewModel.getChatRoomId(megaUser.handle).observe(viewLifecycleOwner) { chatId ->
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    action = ACTION_CHAT_SHOW_MESSAGES
                    putExtra(CHAT_ID, chatId)
                }
                startActivity(intent)
                dismiss()
            }
        }

        binding.optionSendFile.setOnClickListener {
            selectFileLauncher.launch(listOf(megaUser))
        }

        binding.optionShareContact.setOnClickListener {
            selectChatLauncher.launch(megaUser)
        }

        binding.optionShareFolder.setOnClickListener {
            selectFolderLauncher.launch(listOf(megaUser))
        }

        binding.optionRemove.setOnClickListener { showRemoveContactDialog(megaUser) }
    }

    /**
     * Show remove contact dialog to allow contact removal.
     *
     * @param megaUser  MegaUser to be removed
     */
    private fun showRemoveContactDialog(megaUser: MegaUser) {
        if (removeContactDialog?.isShowing == true) removeContactDialog?.dismiss()

        removeContactDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getQuantityString(R.plurals.title_confirmation_remove_contact, 1))
            .setMessage(resources.getQuantityString(R.plurals.confirmation_remove_contact, 1))
            .setNegativeButton(R.string.general_cancel, null)
            .setPositiveButton(R.string.general_remove) { _, _ ->
                viewModel.removeContact(megaUser)
                dismiss()
            }
            .show()
    }

    /**
     * Show node permission dialog to ask for Node permissions.
     */
    private fun showNodePermissionsDialog() {
        if (nodePermissionsDialog?.isShowing == true) nodePermissionsDialog?.dismiss()

        val node = megaApi.getNodeByHandle(folderHandle!!) // TODO Remove `MegaApi` calls in UI
        if (node?.isFolder == true) {
            val permissions = arrayOf(
                getString(R.string.file_properties_shared_folder_read_only),
                getString(R.string.file_properties_shared_folder_read_write),
                getString(R.string.file_properties_shared_folder_full_access)
            )

            nodePermissionsDialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.file_properties_shared_folder_permissions))
                .setSingleChoiceItems(permissions, INVALID_ITEM) { dialog: DialogInterface, item: Int ->
                    NodeController(requireContext()).shareFolder(node, selectedContacts, item)
                    folderHandle = null
                    selectedContacts = null

                    dialog.dismiss()
                    dismiss()
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
