package mega.privacy.android.app.main.adapters

import android.content.Context
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.util.size
import androidx.recyclerview.widget.RecyclerView
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils.dispose
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.main.VersionsFileActivity
import mega.privacy.android.app.main.adapters.VersionsFileAdapter.ViewHolderVersion
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaNodeUtil.getFileInfo
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest.Companion.fromHandle
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

class VersionsFileAdapter(
    var context: Context,
    var listFragment: RecyclerView,
) : RecyclerView.Adapter<ViewHolderVersion?>(), View.OnClickListener, OnLongClickListener,
    DragThumbnailGetter {
    private val megaApi: MegaApiAndroid = MegaApplication.getInstance().megaApi
    var nodes: ArrayList<MegaNode>? = null
        set(value) {
            Timber.d("Setting nodes: ${value?.size}")
            field = value
            notifyDataSetChanged()
        }
    var parentHandle: Long = -1

    private var selectedItems: SparseBooleanArray = SparseBooleanArray()

    var multipleSelect: Boolean = false
        set(value) {
            Timber.d("multipleSelect: $field")
            if (field != value) {
                field = value
            }
            if (field) {
                selectedItems = SparseBooleanArray()
            }
        }

    override fun getNodePosition(handle: Long): Int {
        val nodes = this.nodes
        if (nodes.isNullOrEmpty()) return Constants.INVALID_POSITION

        for (i in nodes.indices) {
            val node = nodes.getOrNull(i)
            if (node != null && node.handle == handle) {
                return i
            }
        }

        return Constants.INVALID_POSITION
    }

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View = viewHolder.itemView

    /* public static view holder class */
    inner class ViewHolderVersion(v: View) : RecyclerView.ViewHolder(v) {
        var document: Long = 0
        val textViewFileName: TextView = v.findViewById(R.id.version_file_filename)
        val textViewFileSize: TextView = v.findViewById(R.id.version_file_filesize)
        val imageView: ImageView = v.findViewById(R.id.version_file_thumbnail)
        val itemLayout: RelativeLayout = v.findViewById(R.id.version_file_item_layout)
        val threeDotsLayout: RelativeLayout = v.findViewById(R.id.version_file_three_dots_layout)
        val headerLayout: RelativeLayout = v.findViewById(R.id.version_file_header_layout)
        val titleHeader: TextView = v.findViewById(R.id.version_file_header_title)
        val sizeHeader: TextView = v.findViewById(R.id.version_file_header_size)
    }

    fun toggleAllSelection(pos: Int) {
        Timber.d("Position: %s", pos)
        val positionToflip = pos

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos)
            selectedItems.delete(pos)
        } else {
            Timber.d("PUT pos: %s", pos)
            selectedItems.put(pos, true)
        }

        val view = listFragment.findViewHolderForLayoutPosition(pos) as ViewHolderVersion?
        if (view != null) {
            Timber.d("Start animation: %d multiselection state: %s", pos, multipleSelect)
            val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
            flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    Timber.d("onAnimationEnd")
                    if (selectedItems.size <= 0) {
                        Timber.d("hideMultipleSelect")
                        (context as VersionsFileActivity).hideMultipleSelect()
                    }
                    Timber.d("notified item changed")
                    notifyItemChanged(positionToflip)
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })
            view.imageView.startAnimation(flipAnimation)
        } else {
            Timber.w("NULL view pos: %s", positionToflip)
            notifyItemChanged(pos)
        }
    }

    fun toggleSelection(pos: Int) {
        Timber.d("Position: %s", pos)

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos)
            selectedItems.delete(pos)
        } else {
            Timber.d("PUT pos: %s", pos)
            selectedItems.put(pos, true)
        }
        notifyItemChanged(pos)

        val view = listFragment.findViewHolderForLayoutPosition(pos) as ViewHolderVersion?
        if (view != null) {
            Timber.d("Start animation: %s", pos)
            val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
            flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    if (selectedItems.size <= 0) {
                        (context as VersionsFileActivity).hideMultipleSelect()
                    }
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }
            })

            view.imageView.startAnimation(flipAnimation)
        } else {
            Timber.w("View is null - not animation")
        }
    }

    /**
     * Selects all Previous Versions and de-select the Current Version
     */
    fun selectAllPreviousVersions() {
        if (isItemChecked(0)) toggleAllSelection(0)
        for (i in 1..<this.getItemCount()) {
            if (!isItemChecked(i)) toggleAllSelection(i)
        }
    }

    /**
     * Checks whether all of the Previous Versions have been selected or not
     *
     * @return true if all Previous Versions have been selected or not, and false if otherwise. It
     * will also return false if only one Version exists (the Current Version), or the Versions list
     * is empty
     */
    fun areAllPreviousVersionsSelected(): Boolean {
        val versionCount = getItemCount()
        if (versionCount <= 1) return false

        for (i in 1..<getItemCount()) {
            if (!isItemChecked(i)) return false
        }
        return true
    }

    fun clearSelections() {
        Timber.d("clearSelections")
        for (i in 0..<this.getItemCount()) {
            if (isItemChecked(i)) {
                toggleAllSelection(i)
            }
        }
    }

    private fun isItemChecked(position: Int): Boolean = selectedItems.get(position, false)

    val selectedNodeVersions: Pair<MutableList<MegaNode>, Boolean>
        /**
         * Retrieves the selected Node Versions
         *
         * @return a Pair containing the List of selected Node Versions and whether the current Version
         * has been selected or not
         */
        get() {
            val nodeVersions = mutableListOf<MegaNode>()
            val selected = selectedItems.size
            val isCurrentVersionSelected: Boolean = selectedItems.get(0, false) == true

            for (i in 0..<selected) {
                if (selectedItems.valueAt(i)) {
                    val position = selectedItems.keyAt(i)
                    val version = getItem(position)
                    if (version != null) {
                        nodeVersions.add(version)
                    }
                }
            }
            return nodeVersions to isCurrentVersionSelected
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderVersion {
        Timber.d("onCreateViewHolder")
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_version_file, parent, false)
        val holderList = ViewHolderVersion(v)
        holderList.itemLayout.tag = holderList
        holderList.itemLayout.setOnClickListener(this)

        when ((context as VersionsFileActivity).accessLevel) {
            MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER -> {
                holderList.itemLayout.setOnLongClickListener(this)
            }

            else -> holderList.itemLayout.setOnLongClickListener(null)
        }

        holderList.threeDotsLayout.tag = holderList
        holderList.threeDotsLayout.setOnClickListener(this)

        v.tag = holderList

        return holderList
    }

    override fun onBindViewHolder(holder: ViewHolderVersion, position: Int) {
        val position = holder.adapterPosition
        Timber.d("Position: %s", position)

        val node = getItem(position) as MegaNode
        holder.document = node.handle

        if (position == 0) {
            holder.titleHeader.text = context.getString(R.string.header_current_section_item)
            holder.sizeHeader.visibility = View.GONE
            holder.headerLayout.visibility = View.VISIBLE
        } else if (position == 1) {
            holder.titleHeader.text = context.resources.getQuantityString(
                R.plurals.header_previous_section_item,
                megaApi.getNumVersions(node)
            )

            if ((context as VersionsFileActivity).versionsSize != null) {
                holder.sizeHeader.text = (context as VersionsFileActivity).versionsSize
                holder.sizeHeader.visibility = View.VISIBLE
            } else {
                holder.sizeHeader.visibility = View.GONE
            }

            holder.headerLayout.visibility = View.VISIBLE
        } else {
            holder.headerLayout.visibility = View.GONE
        }

        holder.textViewFileName.text = node.name
        holder.textViewFileSize.text = ""

        val fileInfo = getFileInfo(node, context)
        holder.textViewFileSize.text = fileInfo

        val paramsLarge = holder.imageView.layoutParams as RelativeLayout.LayoutParams
        paramsLarge.height = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            context.resources.displayMetrics
        ).toInt()
        paramsLarge.width = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            48f,
            context.resources.displayMetrics
        ).toInt()
        val leftLarge = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            12f,
            context.resources.displayMetrics
        ).toInt()
        paramsLarge.setMargins(leftLarge, 0, 0, 0)

        val iconRes = typeForName(node.name).iconResourceId

        if (!multipleSelect) {
            Timber.d("Not multiselect")
            holder.itemLayout.background = null
            holder.imageView.setImageResource(iconRes)
            holder.imageView.layoutParams = paramsLarge

            Timber.d("Check the thumb")
            dispose(holder.imageView)
            if (node.hasThumbnail()) {
                Timber.d("Node has thumbnail")
                val placeholder = ContextCompat.getDrawable(context, iconRes)?.asImage()
                val imageBuilder = ImageRequest.Builder(context)
                    .placeholder(placeholder)
                    .data(fromHandle(node.handle))
                    .target { drawable ->
                        holder
                            .imageView
                            .setImageDrawable(drawable.asDrawable(context.resources))
                    }
                    .transformations(
                        RoundedCornersTransformation(
                            context.resources.getDimension(
                                R.dimen.thumbnail_corner_radius
                            )
                        )
                    )
                    .listener(object : ImageRequest.Listener {
                        override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                            val params =
                                holder.imageView.layoutParams as RelativeLayout.LayoutParams
                            params.height = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                36f,
                                context.resources.displayMetrics
                            ).toInt()
                            params.width = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                36f,
                                context.resources.displayMetrics
                            ).toInt()
                            val left = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                18f,
                                context.resources.displayMetrics
                            ).toInt()
                            val right = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                6f,
                                context.resources.displayMetrics
                            ).toInt()
                            params.setMargins(left, 0, right, 0)
                            holder.imageView.layoutParams = params
                        }
                    })
                    .build()

                SingletonImageLoader.get(context).enqueue(imageBuilder)
            } else {
                Timber.d("Node NOT thumbnail")
                holder.imageView.setImageResource(iconRes)
            }
        } else {
            Timber.d("Multiselection ON")
            if (this.isItemChecked(position)) {
                holder.imageView.layoutParams = paramsLarge
                holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder)
            } else {
                holder.itemLayout.background = null

                Timber.d("Check the thumb")
                holder.imageView.layoutParams = paramsLarge

                dispose(holder.imageView)
                if (node.hasThumbnail()) {
                    Timber.d("Node has thumbnail")
                    val placeholder = ContextCompat.getDrawable(context, iconRes)?.asImage()
                    val imageRequest = ImageRequest.Builder(context)
                        .placeholder(placeholder)
                        .data(fromHandle(node.handle))
                        .target { drawable ->
                            holder
                                .imageView
                                .setImageDrawable(drawable.asDrawable(context.resources))
                        }
                        .transformations(
                            RoundedCornersTransformation(
                                context.resources.getDimension(
                                    R.dimen.thumbnail_corner_radius
                                )
                            )
                        )
                        .listener(object : ImageRequest.Listener {
                            override fun onSuccess(
                                request: ImageRequest,
                                result: SuccessResult,
                            ) {
                                val params =
                                    holder.imageView.layoutParams as RelativeLayout.LayoutParams
                                params.height = TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    36f,
                                    context.resources.displayMetrics
                                ).toInt()
                                params.width = TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    36f,
                                    context.resources.displayMetrics
                                ).toInt()
                                val left = TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    18f,
                                    context.resources.displayMetrics
                                ).toInt()
                                val right = TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    6f,
                                    context.resources.displayMetrics
                                ).toInt()
                                params.setMargins(left, 0, right, 0)
                                holder.imageView.layoutParams = params
                            }
                        })
                        .build()

                    SingletonImageLoader.get(context).enqueue(imageRequest)
                } else {
                    Timber.d("Node NOT thumbnail")
                    holder.imageView.setImageResource(iconRes)
                }
            }
        }
    }

    override fun getItemCount(): Int = nodes?.size ?: 0

    fun getItem(position: Int): MegaNode? = nodes?.getOrNull(position)

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onClick(v: View) {
        Timber.d("onClick")

        val holder = v.tag as ViewHolderVersion
        val currentPosition = holder.adapterPosition
        Timber.d("Current position: %s", currentPosition)

        if (currentPosition < 0) {
            Timber.e("Current position error - not valid value")
            return
        } else if (multipleSelect && currentPosition < 1) {
            Timber.e("Current Version cannot be selected when Multiple Select is activated")
            return
        }

        val megaNode = getItem(currentPosition)
        val id = v.id
        if (id == R.id.version_file_three_dots_layout) {
            Timber.d("version_file_three_dots: %s", currentPosition)
            if (!Util.isOnline(context)) {
                (context as VersionsFileActivity).showSnackbar(context.getString(R.string.error_server_connection_problem))
                return
            }

            if (multipleSelect) {
                (context as VersionsFileActivity).itemClick(currentPosition)
            } else {
                (context as VersionsFileActivity).showVersionsBottomSheetDialog(
                    megaNode,
                    currentPosition
                )
            }
        } else if (id == R.id.version_file_item_layout) {
            (context as VersionsFileActivity).itemClick(currentPosition)
        }
    }

    override fun onLongClick(view: View): Boolean {
        val holder = view.tag as ViewHolderVersion
        val currentPosition = holder.adapterPosition

        if (!multipleSelect) {
            // The Current Version is not allowed to be long-pressed
            if (currentPosition < 1) {
                Timber.w("Position not valid: %s", currentPosition)
            } else {
                multipleSelect = true
                (context as VersionsFileActivity).startActionMode(currentPosition)
            }
        }

        return true
    }
}