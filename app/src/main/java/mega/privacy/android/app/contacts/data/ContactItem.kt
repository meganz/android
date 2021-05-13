package mega.privacy.android.app.contacts.data

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.view.TextDrawable

data class ContactItem constructor(
    val handle: Long,
    val email: String,
    val name: String? = null,
    val status: Int? = null,
    @ColorRes val statusColor: Int? = null,
    var imageUri: Uri? = null,
    @ColorInt val imageColor: Int? = null,
    val lastSeen: String? = null,
    val isNew: Boolean = false
) {

    fun getFirstCharacter(): String? =
        name?.firstOrNull()?.toString()

    fun getPlaceholderDrawable(resources: Resources): Drawable? =
        if (imageColor != null && !name.isNullOrBlank()) {
            TextDrawable.builder()
                .beginConfig()
                .fontSize(resources.getDimensionPixelSize(R.dimen.placeholder_contact_text_size))
                .bold()
                .toUpperCase()
                .endConfig()
                .buildRound(getFirstCharacter(), imageColor)
        } else {
            null
        }

    class DiffCallback : DiffUtil.ItemCallback<ContactItem>() {

        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem == newItem
    }
}
