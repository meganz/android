package mega.privacy.android.app.utils

import android.widget.ImageView

fun getThumbnailLocationOnScreen(imageView: ImageView): IntArray {
    val topLeft = IntArray(2)
    imageView.getLocationOnScreen(topLeft)
    return intArrayOf(topLeft[0], topLeft[1], imageView.width, imageView.height)
}
