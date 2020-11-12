package mega.privacy.android.app.audioplayer

import android.net.Uri
import androidx.databinding.BindingAdapter
import com.facebook.drawee.view.SimpleDraweeView
import mega.privacy.android.app.utils.FileUtil.isFileAvailable
import java.io.File

@BindingAdapter("thumbnail", "defaultThumbnail")
fun setPlaylistItemThumbnail(
    imageView: SimpleDraweeView,
    file: File?,
    defaultThumbnail: Int
) {
    with(imageView) {
        when {
            isFileAvailable(file) -> {
                setImageURI(Uri.fromFile(file))
            }
            else -> {
                setActualImageResource(defaultThumbnail)
            }
        }
    }
}
