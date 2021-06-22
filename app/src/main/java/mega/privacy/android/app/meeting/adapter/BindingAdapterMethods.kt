package mega.privacy.android.app.meeting.adapter

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import mega.privacy.android.app.R

/**
 * Binding Adapter method for Meeting Participant Bottom Sheet page
 */
object BindingAdapterMethods {

    /**
     * Determine if the item should show moderator icon on the participant item
     */
    @JvmStatic
    @BindingAdapter("android:showModeratorIcon")
    fun showModeratorIcon(view: TextView, moderator: Boolean) {
        if (moderator) {
            val drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_moderator)
            drawable?.setTint(ContextCompat.getColor(view.context, R.color.teal_300_teal_200))
            view.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                drawable,
                null
            )
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }

    /**
     * Determine if the item should visible
     */
    @JvmStatic
    @BindingAdapter("android:show")
    fun isVisible(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Determine if the item should visible
     */
    @JvmStatic
    @BindingAdapter("android:hide")
    fun isHidden(view: View, hide: Boolean) {
        view.visibility = if (!hide) View.INVISIBLE else View.VISIBLE
    }
}