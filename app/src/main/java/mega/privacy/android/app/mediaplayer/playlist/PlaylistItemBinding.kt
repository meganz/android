package mega.privacy.android.app.mediaplayer.playlist

import android.net.Uri
import android.widget.FrameLayout
import androidx.databinding.BindingAdapter
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import java.io.File

/**
 * DataBinding implementation for set thumbnail and defaultThumbnail.
 */
@BindingAdapter("apThumbnail", "apDefaultThumbnail")
fun setPlaylistItemThumbnail(
    imageView: SimpleDraweeView,
    file: File?,
    defaultThumbnail: Int
) {
    with(imageView) {
        when {
            isFileAvailable(file) -> {
                setImageURI(Uri.fromFile(file))

                val param = layoutParams as FrameLayout.LayoutParams
                param.width =
                    resources.getDimensionPixelSize(R.dimen.non_default_thumbnail_size)
                param.height = param.width
                param.marginStart =
                    resources.getDimensionPixelSize(R.dimen.non_default_thumbnail_margin_start)
                layoutParams = param
            }
            else -> {
                setActualImageResource(defaultThumbnail)

                val param = layoutParams as FrameLayout.LayoutParams
                param.width =
                    resources.getDimensionPixelSize(R.dimen.default_thumbnail_size)
                param.height = param.width
                param.marginStart =
                    resources.getDimensionPixelSize(R.dimen.default_thumbnail_margin_start)
                layoutParams = param
            }
        }
    }
}
