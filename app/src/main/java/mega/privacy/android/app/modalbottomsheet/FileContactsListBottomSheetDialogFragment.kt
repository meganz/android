package mega.privacy.android.app.modalbottomsheet

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.Util
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaShare.ACCESS_UNKNOWN
import nz.mega.sdk.MegaUser

/**
 * FileContactsListBottomSheetDialogFragment
 */
class FileContactsListBottomSheetDialogFragment : BaseBottomSheetDialogFragment,
    View.OnClickListener {
    private var contact: MegaUser? = null
    private var share: MegaShare? = null
    private var nonContactEmail: String? = null
    private var node: MegaNode? = null
    private var listener: FileContactsListBottomSheetDialogListener? = null

    /**
     * This constructor shouldn't be used, is just here to avoid crashes on recreation. Fragment will be automatically dismissed.
     * This is a temporal fix while this dialog is migrated to compose
     *
     */
    @Deprecated("use other constructor")
    constructor()

    constructor(
        share: MegaShare?,
        contact: MegaUser?,
        node: MegaNode?,
        listener: FileContactsListBottomSheetDialogListener?,
    ) {
        this.share = share
        this.contact = contact
        this.node = node
        this.listener = listener
        if (this.contact == null) {
            nonContactEmail = this.share?.user
        }
    }

    constructor(
        share: MegaShare?,
        node: MegaNode?,
        listener: FileContactsListBottomSheetDialogListener?,
    ) {
        this.share = share
        this.node = node
        this.listener = listener
    }

    /**
     * On create
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (share == null || node == null || listener == null) {
            dismissAllowingStateLoss()
        }
    }

    /**
     * On create view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        contentView = View.inflate(context, R.layout.bottom_sheet_file_contact_list, null)
        itemsLayout = contentView.findViewById(R.id.items_layout)
        if (contact == null) {
            contact = megaApi.getContact(share?.user ?: "")
        }
        if (this.contact == null) {
            nonContactEmail = share?.user
        }

        if (savedInstanceState != null) {
            val email = savedInstanceState.getString(Constants.EMAIL)
            if (email != null) {
                contact = megaApi.getContact(email)
                if (contact == null) {
                    nonContactEmail = email
                }
            }
        }
        return contentView
    }

    /**
     * On dismiss
     */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.fileContactsDialogDismissed()
    }

    /**
     * On View created
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val titleNameContactPanel =
            contentView.findViewById<EmojiTextView>(R.id.file_contact_list_contact_name_text)
        val titleMailContactPanel =
            contentView.findViewById<TextView>(R.id.file_contact_list_contact_mail_text)
        val contactImageView =
            contentView.findViewById<RoundedImageView>(R.id.sliding_file_contact_list_thumbnail)

        val optionChangePermissions =
            contentView.findViewById<TextView>(R.id.file_contact_list_option_permissions)
        val optionDelete = contentView.findViewById<TextView>(R.id.file_contact_list_option_delete)
        val optionInfo = contentView.findViewById<TextView>(R.id.file_contact_list_option_info)

        optionChangePermissions.setOnClickListener(this)
        optionDelete.setOnClickListener(this)
        optionInfo.setOnClickListener(this)

        titleNameContactPanel.setMaxWidthEmojis(Util.scaleWidthPx(200, resources.displayMetrics))
        titleMailContactPanel.maxWidth =
            Util.scaleWidthPx(200, resources.displayMetrics)

        val separatorInfo = contentView.findViewById<View>(R.id.separator_info)
        val separatorChangePermissions =
            contentView.findViewById<View>(R.id.separator_change_permissions)

        val fullName =
            if (contact != null) ContactUtil.getMegaUserNameDB(contact) else nonContactEmail ?: ""

        if (contact?.visibility == MegaUser.VISIBILITY_VISIBLE) {
            optionInfo.visibility = View.VISIBLE
            separatorInfo.visibility = View.VISIBLE
        } else {
            optionInfo.visibility = View.GONE
            separatorInfo.visibility = View.GONE
        }

        titleNameContactPanel.text = fullName
        AvatarUtil.setImageAvatar(
            contact?.handle ?: Constants.INVALID_ID.toLong(),
            contact?.email ?: nonContactEmail,
            fullName,
            contactImageView
        )

        if (share != null) {
            val accessLevel = share?.access ?: ACCESS_UNKNOWN
            when (accessLevel) {
                MegaShare.ACCESS_OWNER, MegaShare.ACCESS_FULL -> titleMailContactPanel.text =
                    getString(R.string.file_properties_shared_folder_full_access)

                MegaShare.ACCESS_READ -> titleMailContactPanel.text =
                    getString(R.string.file_properties_shared_folder_read_only)

                MegaShare.ACCESS_READWRITE -> titleMailContactPanel.text =
                    getString(R.string.file_properties_shared_folder_read_write)
            }
            if (share?.isPending == true) {
                titleMailContactPanel.append(" " + getString(R.string.pending_outshare_indicator))
            }
        } else {
            titleMailContactPanel.text = contact?.email ?: nonContactEmail
        }

        // Disable changing permissions if the node came from Backups
        if (node != null && megaApi.isInInbox(node)) {
            optionChangePermissions.visibility = View.GONE
            separatorChangePermissions.visibility = View.GONE
        }

        super.onViewCreated(view, savedInstanceState)
    }

    /**
     * On click
     */
    override fun onClick(v: View) {
        val id = v.id
        when (id) {
            R.id.file_contact_list_option_permissions -> {
                (contact?.email ?: nonContactEmail)?.let {
                    listener?.changePermissions(it)
                }
            }

            R.id.file_contact_list_option_delete -> {
                (contact?.email ?: nonContactEmail)?.let {
                    listener?.removeFileContactShare(it)
                }
            }

            R.id.file_contact_list_option_info -> {
                share?.user?.let {
                    ContactUtil.openContactInfoActivity(requireActivity(), it)
                }
            }
        }

        setStateBottomSheetBehaviorHidden()
    }

    /**
     * On save instance state
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val email = contact?.email ?: nonContactEmail
        outState.putString(Constants.EMAIL, email)
    }
}
