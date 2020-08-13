package mega.privacy.android.app.fragments.photos

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel
import mega.privacy.android.app.R
import java.io.File

@BindingAdapter("items")
fun setItems(listView: RecyclerView, items: List<PhotoNode>?) {
    items?.let {
        (listView.adapter as PhotosGridAdapter).submitList(items)
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
            imageView.context, shapeId, 0
        ).build()

        Glide.with(imageView).load(file).placeholder(R.drawable.ic_image_thumbnail)
            .error(R.drawable.ic_image_thumbnail).into(this)
    }
}