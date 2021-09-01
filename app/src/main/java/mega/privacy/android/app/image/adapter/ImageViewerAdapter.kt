package mega.privacy.android.app.image.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import mega.privacy.android.app.databinding.PageImageViewerBinding
import mega.privacy.android.app.image.data.ImageItem

class ImageViewerAdapter(
    private val itemCallback: (Long) -> Unit
) : ListAdapter<ImageItem, ImageViewHolder>(ImageItem.DiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = PageImageViewerBinding.inflate(layoutInflater, parent, false)
        return ImageViewHolder(binding)
//            .apply {
//            binding.root.setOnClickListener {
//                itemCallback.invoke(getItem(adapterPosition).handle)
//            }
//        }
//        return ImageViewHolder(
//            PageImageViewerBinding.inflate(LayoutInflater.from(parent.context), parent, false),
//        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long =
        getItem(position).handle
}
