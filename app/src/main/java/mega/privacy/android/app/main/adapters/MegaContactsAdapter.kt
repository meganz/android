package mega.privacy.android.app.main.adapters

import android.app.Activity
import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaContactAdapter
import mega.privacy.android.app.R
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.main.adapters.mapper.MegaContactAdapterToContactItemUiStateMapper
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.utils.AvatarUtil.getColorAvatar
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.FileUtil.JPG_EXTENSION
import mega.privacy.android.shared.contact.model.ContactItemUiState
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaUser
import timber.log.Timber

class MegaContactsAdapter @VisibleForTesting internal constructor(
    private val context: Context,
    contacts: ArrayList<MegaContactAdapter>,
    listFragment: RecyclerView,
    private var adapterType: Int,
    private val megaApi: MegaApiAndroid,
    private val megaChatApi: MegaChatApiAndroid,
    private val contactItemUiStateMapper: MegaContactAdapterToContactItemUiStateMapper,
) : RecyclerView.Adapter<MegaContactsAdapter.ViewHolderContacts>(),
    SectionTitleProvider {

    constructor(
        context: Context,
        contacts: ArrayList<MegaContactAdapter>,
        listFragment: RecyclerView,
        adapterType: Int,
    ) : this(
        context = context,
        contacts = contacts,
        listFragment = listFragment,
        adapterType = adapterType,
        megaApi = ((context as Activity).application as MegaApplication).megaApi,
        megaChatApi = (context.application as MegaApplication).megaChatApi,
        contactItemUiStateMapper = EntryPointAccessors.fromApplication(
            context.applicationContext,
            MegaContactsAdapterEntryPoint::class.java,
        ).megaContactAdapterToContactItemUiStateMapper(),
    )

    var contacts: ArrayList<MegaContactAdapter> = contacts
        set(value) {
            field = value
            positionClicked = -1
            notifyDataSetChanged()
        }

    var positionClicked: Int = -1
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var listFragment: RecyclerView? = listFragment

    private var multipleSelect: Boolean = false
    private var selectedItems: SparseBooleanArray? = null

    override fun getSectionTitle(position: Int, context: Context?): String =
        contacts[position].fullName?.substring(0, 1)?.uppercase().orEmpty()

    /** View holder hosting the contact-row `ComposeView` and the trailing controls. */
    inner class ViewHolderContacts(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemLayout: RelativeLayout = itemView.findViewById(R.id.contact_list_item_layout)
        val contactComposeView: ComposeView = itemView.findViewById(R.id.contact_compose_view)
        val threeDotsLayout: RelativeLayout = itemView.findViewById(R.id.contact_list_three_dots_layout)
        val declineLayout: RelativeLayout = itemView.findViewById(R.id.contact_list_decline)

        @VisibleForTesting
        internal val rowState: ShareContactRowState = ShareContactRowState()
        private var contentInstalled: Boolean = false

        var contactMail: String? = null

        fun setContact(uiState: ContactItemUiState, isSelected: Boolean) {
            rowState.uiState = uiState
            rowState.isSelected = isSelected
            if (!contentInstalled) {
                bindShareContactRow(contactComposeView, rowState)
                contentInstalled = true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderContacts {
        Timber.d("onCreateViewHolder")
        val rowView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_list_compose, parent, false)
        return ViewHolderContacts(rowView).apply {
            contactComposeView.setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            // Route taps on the ComposeView through Compose's own click
            // handling — its pointer-input handler consumes touches and the
            // View-level OnClickListener below never fires for taps inside
            // the ComposeView area.
            rowState.onClick = { onRowClick(this) }
            // Kept as a fallback for taps on the trailing area of the row
            // that fall outside the ComposeView.
            itemLayout.setOnClickListener { onRowClick(this) }
            declineLayout.setOnClickListener { onRowClick(this) }
            threeDotsLayout.visibility = View.GONE
        }
    }

    override fun onBindViewHolder(holder: ViewHolderContacts, position: Int) {
        Timber.d("Position: %s", position)
        val contact = contacts[position]
        holder.contactMail = contact.megaUser?.email

        when (adapterType) {
            ITEM_VIEW_TYPE_LIST_ADD_CONTACT -> {
                holder.declineLayout.visibility = View.GONE
            }

            ITEM_VIEW_TYPE_LIST_GROUP_CHAT -> {
                holder.declineLayout.visibility =
                    if (holder.contactMail == megaApi.myEmail) View.GONE else View.VISIBLE
            }
        }

        val avatarFile = buildAvatarFile(contact.megaUser?.email + JPG_EXTENSION)
            ?.takeIf { it.exists() && it.length() > 0 }
        val rowSelected = adapterType == ITEM_VIEW_TYPE_LIST_ADD_CONTACT && contact.isSelected
        val isVerified = !isItemChecked(position) &&
            megaApi.areCredentialsVerified(contact.megaUser)

        holder.setContact(
            uiState = contactItemUiStateMapper(
                megaContact = contact,
                mail = contact.megaUser?.email.orEmpty(),
                avatarFile = avatarFile,
                avatarColorArgb = getColorAvatar(contact.megaUser),
                chatStatusValue = megaChatApi.getUserOnlineStatus(
                    contact.megaUser?.handle ?: 0L,
                ),
                isVerified = isVerified,
            ),
            isSelected = rowSelected,
        )
    }

    override fun getItemCount(): Int = contacts.size

    override fun getItemViewType(position: Int): Int = adapterType

    fun getItem(position: Int): Any {
        Timber.d("Position: %s", position)
        return contacts[position]
    }

    override fun getItemId(position: Int): Long = position.toLong()

    fun setAdapterType(adapterType: Int) {
        this.adapterType = adapterType
    }

    fun isMultipleSelect(): Boolean = multipleSelect

    fun setMultipleSelect(multipleSelect: Boolean) {
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect
        }
        if (this.multipleSelect) {
            selectedItems = SparseBooleanArray()
        }
    }

    fun toggleSelection(pos: Int) {
        Timber.d("Position: %s", pos)
        val items = selectedItems ?: return
        if (items.get(pos, false)) {
            items.delete(pos)
        } else {
            items.put(pos, true)
        }
        notifyItemChanged(pos)
    }

    fun selectAll() {
        for (i in 0 until itemCount) {
            if (!isItemChecked(i)) {
                toggleSelection(i)
            }
        }
    }

    fun clearSelections() {
        for (i in 0 until itemCount) {
            if (isItemChecked(i)) {
                toggleSelection(i)
            }
        }
    }

    fun clearSelectionsNoAnimations() {
        val items = selectedItems ?: return
        for (i in 0 until itemCount) {
            if (isItemChecked(i)) {
                items.delete(i)
                notifyItemChanged(i)
            }
        }
    }

    private fun isItemChecked(position: Int): Boolean =
        selectedItems?.get(position) == true

    fun getSelectedItemCount(): Int = selectedItems?.size() ?: 0

    fun getSelectedItems(): List<Int> {
        val items = selectedItems ?: return emptyList()
        val result = ArrayList<Int>(items.size())
        for (i in 0 until items.size()) {
            result.add(items.keyAt(i))
        }
        return result
    }

    fun getSelectedUsers(): ArrayList<MegaUser> {
        val users = ArrayList<MegaUser>()
        val items = selectedItems ?: return users
        for (i in 0 until items.size()) {
            if (items.valueAt(i)) {
                getContactAt(items.keyAt(i))?.let { users.add(it) }
            }
        }
        return users
    }

    fun getContactAt(position: Int): MegaUser? = try {
        contacts.getOrNull(position)?.megaUser
    } catch (e: IndexOutOfBoundsException) {
        Timber.e(e)
        null
    }

    private fun onRowClick(holder: ViewHolderContacts) {
        Timber.d("adapterType: %s", adapterType)
        val currentPosition = holder.bindingAdapterPosition
        if (currentPosition == RecyclerView.NO_POSITION) return
        try {
            val c = contacts[currentPosition]
            (context as AddContactActivity).itemClick(c.megaUser?.email.orEmpty(), adapterType)
        } catch (e: IndexOutOfBoundsException) {
            Timber.e(e)
        }
    }

    fun getDocumentAt(position: Int): MegaUser? =
        if (position < contacts.size) contacts[position].megaUser else null

    fun updateContactStatus(position: Int) {
        Timber.d("Position: %s", position)
        notifyItemChanged(position)
    }

    companion object {
        const val ITEM_VIEW_TYPE_LIST_ADD_CONTACT: Int = 0
        const val ITEM_VIEW_TYPE_LIST_GROUP_CHAT: Int = 1
    }
}
