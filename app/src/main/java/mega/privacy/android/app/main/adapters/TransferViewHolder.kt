package mega.privacy.android.app.main.adapters

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * The view holder for transfer section
 */
class TransferViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    /**
     * Thumbnail icon
     */
    lateinit var thumbnailIcon: ImageView

    /**
     * Default icon
     */
    lateinit var defaultIcon: ImageView

    /**
     * The download or upload icon
     */
    lateinit var iconDownloadUploadView: ImageView

    /**
     * Filename
     */
    lateinit var textViewFileName: TextView

    /**
     * Completed icon
     */
    lateinit var imageViewCompleted: ImageView

    /**
     * Completed text
     */
    lateinit var textViewCompleted: TextView

    /**
     * The item layout
     */
    lateinit var itemLayout: RelativeLayout

    /**
     * Reorder icon
     */
    lateinit var optionReorder: ImageView

    /**
     * Pause/play icon
     */
    lateinit var optionPause: ImageView

    /**
     * Mega node handle
     */
    @JvmField
    var document: Long = 0

    /**
     * The path of the current item
     */
    lateinit var currentPath: String

    /**
     * The progress text
     */
    lateinit var progressText: TextView

    /**
     * The speed text
     */
    lateinit var speedText: TextView
}