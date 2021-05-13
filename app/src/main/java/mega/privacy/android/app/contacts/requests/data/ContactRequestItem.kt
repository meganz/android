package mega.privacy.android.app.contacts.requests.data

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.view.TextDrawable

data class ContactRequestItem constructor(
    val handle: Long,
    val email: String? = null,
    val name: String? = null,
    var imageUri: Uri? = null,
    @ColorInt val imageColor: Int? = null,
    val createdTime: String? = null,
    val isOutgoing: Boolean = true
) {

    fun getFirstCharacter(): String? =
        name?.firstOrNull()?.toString() ?: email?.firstOrNull()?.toString()

    fun getPlaceholderDrawable(resources: Resources): Drawable? =
        if (imageColor != null && getFirstCharacter() != null) {
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

    class DiffCallback : DiffUtil.ItemCallback<ContactRequestItem>() {

        override fun areItemsTheSame(oldItem: ContactRequestItem, newItem: ContactRequestItem): Boolean =
            oldItem.handle == newItem.handle

        override fun areContentsTheSame(oldItem: ContactRequestItem, newItem: ContactRequestItem): Boolean =
            oldItem == newItem
    }
}
