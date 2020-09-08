package mega.privacy.android.app.fragments.homepage

import android.widget.ImageView

fun ImageView.getLocationAndDimen(): IntArray {
    val topLeft = IntArray(2)
    getLocationOnScreen(topLeft)
    return intArrayOf(topLeft[0], topLeft[1], width, height)
}