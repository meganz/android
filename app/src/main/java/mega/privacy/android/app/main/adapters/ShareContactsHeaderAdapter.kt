package mega.privacy.android.app.main.adapters

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.main.ShareContactInfo
import mega.privacy.android.app.main.adapters.ShareContactsHeaderAdapter.ViewHolderShareContacts
import mega.privacy.android.app.main.adapters.mapper.ShareContactInfoToContactItemUiStateMapper
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.Constants.HEADER_VIEW_TYPE
import mega.privacy.android.app.utils.Constants.ITEM_PROGRESS
import mega.privacy.android.app.utils.Constants.ITEM_VIEW_TYPE
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.shared.contact.model.ContactItemUiState
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaUser
import java.util.Locale

class ShareContactsHeaderAdapter @VisibleForTesting internal constructor(
    private val context: Context,
    private var shareContacts: List<ShareContactInfo>?,
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val contactItemUiStateMapper: ShareContactInfoToContactItemUiStateMapper,
) : RecyclerView.Adapter<ViewHolderShareContacts>(), SectionTitleProvider {

    constructor(context: Context, shareContacts: ArrayList<ShareContactInfo>?) : this(
        context = context,
        shareContacts = shareContacts,
        megaApi = ((context as Activity).application as MegaApplication).megaApi,
        megaChatApi = (context.application as MegaApplication).megaChatApi,
        contactItemUiStateMapper = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ShareContactsHeaderAdapterEntryPoint::class.java,
        ).shareContactInfoToContactItemUiStateMapper(),
    )

    var onItemClickListener: OnItemClickListener? = null
    var onLongItemClickListener: OnLongItemClickListener? = null

    fun setContacts(contacts: List<ShareContactInfo>?) {
        shareContacts = contacts
        notifyDataSetChanged()
    }

    fun getItem(position: Int): ShareContactInfo? =
        shareContacts?.getOrNull(position)

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getSectionTitle(position: Int, context: Context?): String? {
        val contact = shareContacts?.getOrNull(position) ?: return null
        if (contact.isHeader) return null
        val source = when {
            contact.isMegaContact -> contact.megaContactAdapter?.fullName
            else -> contact.phoneContactInfo?.name
        }
        return source?.firstOrNull()?.toString()?.uppercase(Locale.getDefault())
    }

    override fun getItemViewType(position: Int): Int {
        val contact = getItem(position) ?: return ITEM_VIEW_TYPE
        return when {
            contact.isHeader -> HEADER_VIEW_TYPE
            contact.isProgress -> ITEM_PROGRESS
            else -> ITEM_VIEW_TYPE
        }
    }

    override fun getItemCount(): Int = shareContacts?.size ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderShareContacts {
        val rowView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_share, parent, false)
        return ViewHolderShareContacts(rowView).apply {
            contactComposeView.setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
        }
    }

    override fun onBindViewHolder(holder: ViewHolderShareContacts, position: Int) {
        val contact = getItem(position) ?: return
        holder.currentPosition = holder.bindingAdapterPosition
        holder.itemProgress.visibility = View.GONE

        when {
            contact.isMegaContact -> bindMegaContact(holder, contact)
            contact.isPhoneContact -> bindPhoneContact(holder, contact)
            contact.isProgress -> showProgress(holder)
        }
    }

    private fun bindMegaContact(holder: ViewHolderShareContacts, contact: ShareContactInfo) {
        if (contact.isHeader) {
            holder.showHeader(context.getString(mega.privacy.android.shared.resources.R.string.general_section_contacts))
            return
        }
        holder.showRow()

        val mail = (context as AddContactActivity).getShareContactMail(contact)
        holder.mail = mail

        val megaUser = contact.megaContactAdapter?.megaUser
        val sdkUser = mail?.let { megaApi.getContact(it) }
        val isVerified = sdkUser != null && megaApi.areCredentialsVerified(sdkUser)

        val avatarFile = megaUser?.let { buildAvatarFile(it.email + FileUtil.JPG_EXTENSION) }
        val avatarColorArgb = AvatarUtil.getColorAvatar(megaUser)
        val chatStatus = megaUser?.let { megaChatApi.getUserOnlineStatus(it.handle) } ?: 0

        holder.setContact(
            uiState = contactItemUiStateMapper(
                info = contact,
                mail = mail.orEmpty(),
                avatarFile = avatarFile,
                avatarColorArgb = avatarColorArgb,
                chatStatusValue = chatStatus,
                isVerified = isVerified,
            ),
            isSelected = contact.megaContactAdapter?.isSelected == true,
        )
    }

    private fun bindPhoneContact(holder: ViewHolderShareContacts, contact: ShareContactInfo) {
        if (contact.isHeader) {
            holder.showHeader(context.getString(R.string.contacts_phone))
            return
        }
        holder.showRow()

        val email = contact.phoneContactInfo?.email.orEmpty()
        val avatarColorArgb = AvatarUtil.getColorAvatar(null as MegaUser?)

        holder.setContact(
            uiState = contactItemUiStateMapper(
                info = contact,
                mail = email,
                avatarFile = null,
                avatarColorArgb = avatarColorArgb,
                chatStatusValue = 0,
                isVerified = false,
            ),
            isSelected = false,
        )
    }

    private fun showProgress(holder: ViewHolderShareContacts) {
        holder.itemLayout.visibility = View.GONE
        holder.itemHeader.visibility = View.GONE
        holder.itemProgress.visibility = View.VISIBLE
    }

    @Suppress("FunctionName")
    fun SetOnItemClickListener(listener: OnItemClickListener?) {
        onItemClickListener = listener
    }

    @Suppress("FunctionName")
    fun SetOnLongItemClickListener(listener: OnLongItemClickListener?) {
        onLongItemClickListener = listener
    }

    inner class ViewHolderShareContacts(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemProgress: RelativeLayout = itemView.findViewById(R.id.item_progress)
        val itemHeader: RelativeLayout = itemView.findViewById(R.id.header)
        val textHeader: TextView = itemView.findViewById(R.id.text_header)
        val itemLayout: ViewGroup = itemView.findViewById(R.id.item_content)
        val contactComposeView: ComposeView = itemView.findViewById(R.id.contact_compose_view)

        @VisibleForTesting
        internal val rowState: ShareContactRowState = ShareContactRowState()
        private var contentInstalled: Boolean = false
        var mail: String? = null
        var currentPosition: Int = 0

        init {
            // Route taps on the ComposeView through Compose's own click
            // handling — its pointer-input handler consumes touches and the
            // View-level OnClickListener below never fires for taps inside
            // the ComposeView area.
            rowState.onClick = {
                onItemClickListener?.onItemClick(itemView, bindingAdapterPosition)
            }
            rowState.onLongClick = {
                onLongItemClickListener?.onLongItemClick(itemView, bindingAdapterPosition)
            }
            // Kept as a fallback for taps that land outside the ComposeView
            // area of the row.
            itemView.setOnClickListener {
                onItemClickListener?.onItemClick(it, bindingAdapterPosition)
            }
            itemView.setOnLongClickListener {
                onLongItemClickListener?.onLongItemClick(it, bindingAdapterPosition)
                onLongItemClickListener != null
            }
        }

        fun setContact(uiState: ContactItemUiState, isSelected: Boolean) {
            rowState.uiState = uiState
            rowState.isSelected = isSelected
            if (!contentInstalled) {
                bindShareContactRow(contactComposeView, rowState)
                contentInstalled = true
            }
        }

        fun showHeader(text: String) {
            itemLayout.visibility = View.GONE
            itemHeader.visibility = View.VISIBLE
            textHeader.text = text
        }

        fun showRow() {
            itemLayout.visibility = View.VISIBLE
            itemHeader.visibility = View.GONE
        }
    }

    fun interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    fun interface OnLongItemClickListener {
        fun onLongItemClick(view: View, position: Int)
    }
}
