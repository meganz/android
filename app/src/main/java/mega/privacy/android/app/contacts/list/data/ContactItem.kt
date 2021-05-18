package mega.privacy.android.app.contacts.list.data

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
    val firstName: String? = null,
    val lastName: String? = null,
    val alias: String? = null,
    val status: Int? = null,
    @ColorRes val statusColor: Int? = null,
    var imageUri: Uri? = null,
    @ColorInt val imageColor: Int? = null,
    val lastSeen: String? = null,
    val isNew: Boolean = false
) {

    fun getFirstCharacter(): String? =
        firstName?.firstOrNull()?.toString()

    fun getPlaceholderDrawable(resources: Resources): Drawable? =
        if (imageColor != null && !firstName.isNullOrBlank()) {
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

    fun matches(queryString: String): Boolean =
        firstName?.contains(queryString, true) == true ||
                lastName?.contains(queryString, true) == true ||
                alias?.contains(queryString, true) == true ||
                email.contains(queryString, true)

    class DiffCallback : DiffUtil.ItemCallback<ContactItem>() {

        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem == newItem
    }
}
