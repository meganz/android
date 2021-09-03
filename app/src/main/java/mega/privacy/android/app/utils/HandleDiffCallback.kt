package mega.privacy.android.app.utils

import androidx.recyclerview.widget.DiffUtil

class HandleDiffCallback : DiffUtil.ItemCallback<Long>() {
    override fun areItemsTheSame(oldItem: Long, newItem: Long) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: Long, newItem: Long) =
        oldItem == newItem
}
