package mega.privacy.android.app.fragments.photos

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import mega.privacy.android.app.R
import java.io.File

@BindingAdapter("app:items")
fun setItems(listView: RecyclerView, items: List<PhotoNode>?) {
    items?.let {
        (listView.adapter as PhotosGridAdapter).submitList(items)
    }
}

@BindingAdapter("app:thumbnail")
fun setThumbnail(imageView: ImageView, file: File?) {
    with(imageView) {
        var requestBuilder = if (file != null) {
            Glide.with(this).load(file)
                .placeholder(R.drawable.ic_image_thumbnail)
        } else {
            Glide.with(this).load(R.drawable.ic_image_thumbnail)
        }
//        requestBuilder = if (node.isSelected()) {
//            requestBuilder
//                .transform(
//                    CenterCrop(),
//                    RoundedCorners(itemSizeConfig.getRoundCornerRadius())
//                )
//        } else {
        requestBuilder = requestBuilder.transform(CenterCrop())
//        }
        requestBuilder
//            .transition(DrawableTransitionOptions.withCrossFade())
            .into(this)


//        val padding = if (node.isSelected()) itemSizeConfig.getSelectedPadding() else 0
        val padding = 0
        setPadding(padding, padding, padding, padding)
    }

}