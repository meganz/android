package mega.privacy.android.app.main.adapters

import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import coil3.ImageLoader
import coil3.asDrawable
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils
import mega.privacy.android.app.FileDocument
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.main.adapters.FileStorageAdapter.ViewHolderFileStorage
import mega.privacy.android.app.presentation.filestorage.FileStorageActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import timber.log.Timber

/*
 * Adapter for FileStorageAdapter list
 */
class FileStorageAdapter(
    private val context: Context,
    private val mode: FileStorageActivity.Mode?,
) : RecyclerView.Adapter<ViewHolderFileStorage>(), View.OnClickListener {

    var currentFiles: List<FileDocument>? = null
        set(value) {
            Timber.d("Setting current files: ${value?.size ?: 0}")
            field = value
            notifyDataSetChanged()
        }

    /*public view holder class*/
    inner class ViewHolderFileStorage(v: View) : RecyclerView.ViewHolder(v) {
        val imageView: ImageView = v.findViewById(R.id.file_explorer_thumbnail)
        val textViewFileName: TextView = v.findViewById(R.id.file_explorer_filename)
        val textViewFileSize: TextView = v.findViewById(R.id.file_explorer_filesize)
        val itemLayout: RelativeLayout = v.findViewById(R.id.file_explorer_item_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderFileStorage {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_explorer, parent, false)
        val holder = ViewHolderFileStorage(v)

        holder.itemLayout.setOnClickListener(this)
        holder.textViewFileName.setOnClickListener(this)
        holder.textViewFileName.tag = holder
        holder.imageView.clipToOutline = true
        v.tag = holder

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolderFileStorage, position: Int) {
        val document = currentFiles?.get(position)

        holder.textViewFileName.text = document?.name

        if (document?.isFolder == true) {
            val childrenCount = document.totalChildren
            val items = context.resources
                .getQuantityString(R.plurals.general_num_items, childrenCount, childrenCount)
            holder.textViewFileSize.text = items
        } else {
            val documentSize = document?.size ?: 0
            holder.textViewFileSize.text = Util.getSizeString(documentSize, context)
        }

        holder.itemLayout.setBackgroundResource(
            if (document?.isHighlighted == true) R.color.color_background_surface_2 else android.R.color.transparent
        )

        resetImageView(holder.imageView)

        val iconRes = typeForName(document?.name).iconResourceId

        when (mode) {
            FileStorageActivity.Mode.PICK_FOLDER -> {
                holder.itemLayout.isEnabled = isEnabled(position)

                if (document?.isFolder == true) {
                    holder.imageView.setImageResource(mega.privacy.android.icon.pack.R.drawable.ic_folder_medium_solid)
                } else {
                    holder.imageView.setImageResource(iconRes)
                }
            }

            FileStorageActivity.Mode.BROWSE_FILES -> {
                if (document != null && document.isFolder) {
                    holder.imageView.setImageResource(mega.privacy.android.icon.pack.R.drawable.ic_folder_medium_solid)
                } else {
                    if (typeForName(document?.name).isImage || typeForName(document?.name).isVideo) {
                        val uri = document?.getUri()
                        val placeholder = ContextCompat.getDrawable(context, iconRes)?.asImage()
                        val imageRequest = ImageRequest
                            .Builder(context)
                            .placeholder(placeholder)
                            .data(uri)
                            .target { drawable ->
                                holder
                                    .imageView
                                    .setImageDrawable(
                                        drawable.asDrawable(context.resources)
                                    )
                            }
                            .transformations(
                                RoundedCornersTransformation(
                                    radius = context.resources.getDimension(R.dimen.thumbnail_corner_radius),
                                )
                            )
                            .build()

                        ImageLoader
                            .Builder(context)
                            .build()
                            .enqueue(imageRequest)
                    } else {
                        holder.imageView.setImageResource(iconRes)
                    }
                }
            }

            else -> return
        }
    }

    /**
     * Reset the imageview's params
     *
     * @param imageView the imageview shows the icon and the thumbnail
     */
    fun resetImageView(imageView: ImageView) {
        val params = imageView.layoutParams as RelativeLayout.LayoutParams
        params.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
        params.height = params.width
        val margin = Util.dp2px(Constants.ICON_MARGIN_DP.toFloat())
        params.setMargins(margin, margin, margin, margin)
        imageView.layoutParams = params
        CoilUtils.dispose(imageView)
    }

    fun getDocumentAt(position: Int): FileDocument? = currentFiles?.getOrNull(position)

    override fun getItemCount(): Int = currentFiles?.size ?: 0

    /**
     * Checks if the view item has to be enabled or not.
     *
     * @param position position of the item
     * @return True if the view item has to be enabled, false otherwise.
     */
    fun isEnabled(position: Int): Boolean {
        val files = currentFiles ?: return false
        val document = files[position]
        return !(mode == FileStorageActivity.Mode.PICK_FOLDER && !document.isFolder)
    }

    override fun onClick(v: View) {
        val holder = v.tag as ViewHolderFileStorage
        val currentPosition = holder.adapterPosition
        val id = v.id
        if (id == R.id.file_explorer_filename || id == R.id.file_explorer_item_layout) {
            (context as FileStorageActivity).itemClick(currentPosition)
        }
    }


    class CenterSmoothScroller(context: Context) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int,
        ): Int =
            // Calculate the distance needed to center the view
            (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2)

        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float =
            50f / displayMetrics.densityDpi
    }
}

