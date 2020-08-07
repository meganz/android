package mega.privacy.android.app.fragments.photos

import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import mega.privacy.android.app.R
import java.io.File

@BindingAdapter("app:items")
fun setItems(listView: RecyclerView, items: List<PhotoNode>?) {
    items?.let {
        Log.i("Alex", "submit list")
        (listView.adapter as PhotosGridAdapter).submitList(items)
    }
}

@BindingAdapter("app:thumbnail")
fun setThumbnail(imageView: ImageView, file: File?) {
    imageView.let {
        var requestBuilder = if (file != null) {
            Glide.with(imageView).load(file)
                .placeholder(R.drawable.ic_image_thumbnail)
        } else {
            Glide.with(imageView).load(R.drawable.ic_image_thumbnail)
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
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)

//        val padding = if (node.isSelected()) itemSizeConfig.getSelectedPadding() else 0
        val padding = 0
        imageView.setPadding(padding, padding, padding, padding)
    }
}