package mega.privacy.android.app.uploadFolder.list.adapter

import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.databinding.ItemGridSeparatorBinding

/**
 * RecyclerView's ViewHolder to show correctly the separation between folders and files.
 *
 * @property binding    Item's view binding
 */
class GridSeparatorHolder(private val binding: ItemGridSeparatorBinding) :
    RecyclerView.ViewHolder(binding.root)