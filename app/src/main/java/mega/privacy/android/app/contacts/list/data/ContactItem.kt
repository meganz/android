package mega.privacy.android.app.contacts.list.data

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.recyclerview.widget.DiffUtil
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.view.TextDrawable
import java.util.*

sealed class ContactItem(val id: Long) {

    data class Data constructor(
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
    ) : ContactItem(handle) {

        fun getTitle(): String =
            firstName ?: email

        fun getFirstCharacter(): String =
            getTitle().first().toString().toUpperCase(Locale.getDefault())

        fun getPlaceholderDrawable(context: Context): Drawable =
            TextDrawable.builder()
                .beginConfig()
                .fontSize(context.resources.getDimensionPixelSize(R.dimen.placeholder_contact_text_size))
                .bold()
                .toUpperCase()
                .endConfig()
                .buildRound(
                    getFirstCharacter(),
                    imageColor ?: getThemeColor(context, R.attr.colorSecondary)
                )

        fun matches(queryString: String): Boolean =
            firstName?.contains(queryString, true) == true ||
                    lastName?.contains(queryString, true) == true ||
                    alias?.contains(queryString, true) == true ||
                    email.contains(queryString, true)
    }

    data class Header(val title: String) : ContactItem(title.hashCode().toLong())

    class DiffCallback : DiffUtil.ItemCallback<ContactItem>() {
        override fun areItemsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ContactItem, newItem: ContactItem): Boolean {
            val isSameDataItem = oldItem is Data && newItem is Data && oldItem == newItem
            val isSameHeaderItem = oldItem is Header && newItem is Header && oldItem == newItem
            return isSameDataItem || isSameHeaderItem
        }
    }
}
