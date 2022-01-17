package mega.privacy.android.app.upload.view

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.upload.data.FolderItem

@Suppress("UNCHECKED_CAST")
@BindingAdapter("items")
fun setItems(listView: RecyclerView, items: List<FolderItem>?) {
    items?.let { folderItems ->
        (listView.adapter as ListAdapter<FolderItem, RecyclerView.ViewHolder>).submitList(
            folderItems
        )
    }
}