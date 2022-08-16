package mega.privacy.android.app.fragments.recent

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.utils.Util

class ViewHolderMultipleBucket(
    itemView: View,
    context: Context,
    private val isMedia: Boolean,
) : RecyclerView.ViewHolder(itemView) {
    private val outMetrics = context.resources.displayMetrics
    private val multipleBucketLayout: LinearLayout? = null
    private val document: Long = 0
    private val mediaView: RelativeLayout? = null
    private val thumbnailMedia: ImageView? = null
    private val videoLayout: RelativeLayout? = null
    private val videoDuration: TextView? = null
    private val listView: RelativeLayout? = null
    private val thumbnailList: ImageView? = null
    private val nameText: TextView? = null
    private val infoText: TextView? = null
    private val imgLabel: ImageView? = null
    private val imgFavourite: ImageView? = null
    private val threeDots: ImageView? = null

    fun getDocument(): Long {
        return document
    }

    fun setImageThumbnail(image: Bitmap?) {
        if (isMedia) {
            (thumbnailMedia ?: return).setImageBitmap(image)
        } else {
            val params = thumbnailList!!.layoutParams as RelativeLayout.LayoutParams
            params.height = Util.dp2px(36f, outMetrics)
            params.width = params.height
            val margin = Util.dp2px(18f, outMetrics)
            params.setMargins(margin, margin, margin, 0)
            thumbnailList.layoutParams = params
            thumbnailList.setImageBitmap(image)
        }
    }

    fun getThumbnailList(): ImageView? {
        return thumbnailList
    }

    fun getThumbnailMedia(): ImageView? {
        return thumbnailMedia
    }
}