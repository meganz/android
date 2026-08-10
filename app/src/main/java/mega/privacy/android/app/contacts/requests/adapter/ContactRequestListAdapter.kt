package mega.privacy.android.app.contacts.requests.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.recyclerview.widget.ListAdapter
import dagger.hilt.android.EntryPointAccessors
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.requests.mapper.ContactRequestItemToContactItemUiStateMapper
import mega.privacy.android.app.databinding.ItemContactRequestBinding
import mega.privacy.android.app.utils.AdapterUtils.isValidPosition
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile
import mega.privacy.android.app.utils.FileUtil

/**
 * RecyclerView's ListAdapter to show ContactRequestItem.
 *
 * @property itemCallback   Callback to be called when an item is clicked.
 */
class ContactRequestListAdapter @VisibleForTesting internal constructor(
    private val itemCallback: (Long) -> Unit,
    private val mapperProvider: (Context) -> ContactRequestItemToContactItemUiStateMapper,
) : ListAdapter<ContactRequestItem, ContactRequestListViewHolder>(ContactRequestItem.DiffCallback()) {

    constructor(itemCallback: (Long) -> Unit) : this(
        itemCallback = itemCallback,
        mapperProvider = { context ->
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ContactRequestListAdapterEntryPoint::class.java,
            ).contactRequestItemToContactItemUiStateMapper()
        },
    )

    private var contactItemUiStateMapper: ContactRequestItemToContactItemUiStateMapper? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactRequestListViewHolder {
        if (contactItemUiStateMapper == null) {
            contactItemUiStateMapper = mapperProvider(parent.context)
        }
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemContactRequestBinding.inflate(layoutInflater, parent, false)
        binding.contactComposeView.setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
        return ContactRequestListViewHolder(binding).apply {
            binding.root.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    itemCallback.invoke(getItem(bindingAdapterPosition).handle)
                }
            }
            binding.btnMore.setOnClickListener {
                if (isValidPosition(bindingAdapterPosition)) {
                    itemCallback.invoke(getItem(bindingAdapterPosition).handle)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ContactRequestListViewHolder, position: Int) {
        val item = getItem(position)
        val avatarFile = buildAvatarFile(item.email + FileUtil.JPG_EXTENSION)
        val avatarColorArgb = AvatarUtil.getColorAvatar(item.handle)
        holder.bind(
            requireNotNull(contactItemUiStateMapper).invoke(
                item = item,
                avatarFile = avatarFile,
                avatarColorArgb = avatarColorArgb,
            )
        )
    }

    override fun getItemId(position: Int): Long =
        getItem(position).handle
}
