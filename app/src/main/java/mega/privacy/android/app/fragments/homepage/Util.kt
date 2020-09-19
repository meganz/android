package mega.privacy.android.app.fragments.homepage

import android.widget.ImageView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

fun ImageView.getLocationAndDimen(): IntArray {
    val topLeft = IntArray(2)
    getLocationOnScreen(topLeft)
    return intArrayOf(topLeft[0], topLeft[1], width, height)
}

fun disableRecyclerViewAnimator(rv: RecyclerView, duration: Long = 100) {
    val oldAnimator = rv.itemAnimator ?: DefaultItemAnimator()
    rv.itemAnimator = null
    rv.postDelayed({ rv.itemAnimator = oldAnimator }, duration)
}
