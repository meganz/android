package mega.privacy.android.app.meeting.adapter

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import mega.privacy.android.app.R

/**
 * Binding Adapter method for Meeting Participant Bottom Sheet page
 */
object MeetingBindingAdapterMethods {

    /**
     * Determine if the item should show moderator icon on the participant item
     *
     * @param view the target view
     * @param moderator whether the view should show the moderator icon
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
     *
     * @param view the target viw
     * @param show whether the view should be shown
     */
    @JvmStatic
    @BindingAdapter("android:show")
    fun isVisible(view: View, show: Boolean) {
        view.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * Determine if the item should be hidden
     *
     * @param view the target view
     * @param hide whether the view should be hidden
     */
    @JvmStatic
    @BindingAdapter("android:hide")
    fun isHidden(view: View, hide: Boolean) {
        view.visibility = if (!hide) View.INVISIBLE else View.VISIBLE
    }
}