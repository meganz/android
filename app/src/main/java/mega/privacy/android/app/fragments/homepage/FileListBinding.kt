package mega.privacy.android.app.fragments.homepage

import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import mega.privacy.android.app.utils.Util
import java.io.File

@Suppress("UNCHECKED_CAST")
@BindingAdapter("items")
fun setItems(listView: RecyclerView, items: List<NodeItem>?) {
    items?.let {
        (listView.adapter as ListAdapter<NodeItem, RecyclerView.ViewHolder>).submitList(it)
    }
}

@BindingAdapter("thumbnail", "selected")
fun setGridItemThumbnail(imageView: SimpleDraweeView, file: File?, selected: Boolean) {
    with(imageView) {
        if (isFileAvailable(file)) {
            setImageURI(Uri.fromFile(file))
        } else {
            setActualImageResource(R.drawable.ic_image_thumbnail)
        }

        hierarchy.roundingParams = if (selected) getRoundingParams(context) else null
    }
}

@BindingAdapter("thumbnail", "item_selected", "defaultThumbnail")
fun setListItemThumbnail(
    imageView: SimpleDraweeView,
    file: File?,
    selected: Boolean,
    defaultThumbnail: Int
) {
    with(imageView) {
        when {
            selected -> {
                setActualImageResource(R.drawable.ic_select_folder)
            }
            isFileAvailable(file) -> {
                setImageURI(Uri.fromFile(file))
            }
            else -> {
                setActualImageResource(defaultThumbnail)
            }
        }
    }
}

@BindingAdapter("thumbnail", "defaultThumbnail")
fun setNodeGridThumbnail(imageView: SimpleDraweeView, file: File?, defaultThumbnail: Int) {
    with(imageView) {
        if (isFileAvailable(file)) {
            setImageURI(Uri.fromFile(file))
        } else {
            setActualImageResource(defaultThumbnail)
        }

        val params = layoutParams
        if (params is FrameLayout.LayoutParams) {
            val realThumbnailSize = resources.getDimensionPixelSize(R.dimen.grid_node_item_width)
            val defaultThumbnailSize =
                resources.getDimensionPixelSize(R.dimen.grid_node_default_thumbnail_size)
            val defaultThumbnailMarginTop = (realThumbnailSize - defaultThumbnailSize) / 2

            params.width =
                if (file == null) defaultThumbnailSize else ViewGroup.LayoutParams.MATCH_PARENT

            params.height = if (file == null) defaultThumbnailSize else realThumbnailSize

            params.topMargin = if (file == null) defaultThumbnailMarginTop else 0

            layoutParams = params
        }

        val radius = resources.getDimensionPixelSize(R.dimen.homepage_node_grid_round_corner_radius)
            .toFloat()
        hierarchy.roundingParams = RoundingParams.fromCornersRadii(radius, radius, 0F, 0F)
    }
}

@BindingAdapter("visibleGone")
fun showHide(view: View, show: Boolean) {
    view.visibility = if (show) View.VISIBLE else View.GONE
}

private var roundingParams: RoundingParams? = null

fun getRoundingParams(context: Context): RoundingParams? {
    roundingParams?.let {
        return it
    }

    roundingParams = RoundingParams.fromCornersRadius(
        Util.dp2px(
            context.resources.getDimension(R.dimen.photo_selected_icon_round_corner_radius),
            context.resources.displayMetrics
        ).toFloat()
    )

    roundingParams?.apply {
        setBorder(
            ContextCompat.getColor(context, R.color.accentColor), Util.dp2px(
                context.resources.getDimension(R.dimen.photo_selected_border_width),
                context.resources.displayMetrics
            ).toFloat()
        )
    }

    return roundingParams
}

