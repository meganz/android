package mega.privacy.android.app.fragments.photos

import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import mega.privacy.android.app.R
import java.io.File

@Suppress("UNCHECKED_CAST")
@BindingAdapter("items")
fun setItems(listView: RecyclerView, items: List<PhotoNode>?) {
    Log.i("Alex", "submitlist")
    items?.let {
        val adapter = listView.adapter
        if (adapter is PhotosBrowseAdapter) {
            adapter.submitList(it)
        } else if (adapter is PhotosSearchAdapter) {
//            adapter.submitList(it.filter { node -> node.type == PhotoNode.TYPE_PHOTO })
            adapter.submitList(it)
        }
    }
}

@BindingAdapter("thumbnail", "selected")
fun setThumbnail(imageView: ShapeableImageView, file: File?, selected: Boolean) {
    val strokeWidth: Float
    val shapeId: Int

    with(imageView) {
        strokeWidth =
            if (selected) resources.getDimension(R.dimen.photo_selected_border_width) else 0f
        shapeId = if (selected) R.style.GalleryImageShape_Selected else R.style.GalleryImageShape

        setStrokeWidth(strokeWidth)
        shapeAppearanceModel = ShapeAppearanceModel.builder(
            context, shapeId, 0
        ).build()

        Glide.with(this).load(file).placeholder(R.drawable.ic_image_thumbnail)
            .error(R.drawable.ic_image_thumbnail)
            /*.transition(DrawableTransitionOptions.withCrossFade())*/.into(this)
    }
}

@BindingAdapter("thumbnail", "selected")
fun setSearchThumbnail(imageView: ImageView, file: File?, selected: Boolean) {
    with(imageView) {
        if (selected) {
            Glide.with(this).load(R.drawable.ic_select_folder).into(this)
        } else {
            Glide.with(this).load(file)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(ROUND_RADIUS)))
                .placeholder(R.drawable.ic_image_thumbnail).error(R.drawable.ic_image_thumbnail)
                .into(this)
        }
    }
}

private const val ROUND_RADIUS = 10

