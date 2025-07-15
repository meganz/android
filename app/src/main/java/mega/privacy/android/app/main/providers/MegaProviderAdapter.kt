package mega.privacy.android.app.main.providers

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.util.SparseBooleanArray
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.size.Scale
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils.dispose
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.main.providers.MegaProviderAdapter.ViewHolderProvider
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.getFileInfo
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest.Companion.fromHandle
import mega.privacy.android.icon.pack.R as IconPackR
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

class MegaProviderAdapter(
    private val context: Context,
    private val fragment: Fragment,
    private val listView: RecyclerView,
    private val type: Int,
) : RecyclerView.Adapter<ViewHolderProvider?>(), View.OnClickListener, OnLongClickListener {
    var megaApi: MegaApiAndroid = MegaApplication.getInstance().megaApi

    @JvmField
    var positionClicked: Int = -1

    @JvmField
    var parentHandle: Long = -1

    @JvmField
    var nodes: ArrayList<MegaNode>? = null

    private var isMultipleSelect: Boolean = false

    private var selectedItems: SparseBooleanArray = SparseBooleanArray()

    private val outMetrics = DisplayMetrics()


    /*public static view holder class*/
    inner class ViewHolderProvider(v: View) : RecyclerView.ViewHolder(v) {
        var imageView: ImageView = v.findViewById(R.id.file_explorer_thumbnail)
        var permissionsIcon: ImageView = v.findViewById(R.id.file_explorer_permissions)
        var textViewFileName: TextView = v.findViewById(R.id.file_explorer_filename)
        var textViewFileSize: TextView = v.findViewById(R.id.file_explorer_filesize)
        var itemLayout: RelativeLayout = v.findViewById(R.id.file_explorer_item_layout)
        var currentPosition: Int = 0
        var document: Long = 0
    }

    init {
        val display = (context as Activity).windowManager.defaultDisplay
        display.getMetrics(outMetrics)
    }

    override fun getItemCount() = nodes?.size ?: 0

    fun getItem(position: Int) = nodes?.getOrNull(position)

    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderProvider {
        Timber.d("onCreateViewHolder")
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_explorer, parent, false)
        val holder = ViewHolderProvider(v)
        val orientation = context.resources.configuration.orientation

        holder.itemLayout.setOnClickListener(this)
        holder.itemLayout.setOnLongClickListener(this)
        holder.textViewFileName.setOnClickListener(this)
        holder.textViewFileName.setOnLongClickListener(this)
        holder.textViewFileName.tag = holder

        val scaleWidth = if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            260
        } else {
            200
        }

        holder.textViewFileSize.maxWidth = Util.scaleWidthPx(scaleWidth, outMetrics)

        v.tag = holder

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolderProvider, position: Int) {
        Timber.d("onBindViewHolder")

        holder.currentPosition = holder.getBindingAdapterPosition()

        val node = getItem(position) as MegaNode
        holder.document = node.handle
        holder.textViewFileName.text = node.name

        Util.setViewAlpha(holder.imageView, 1f)

        val params = holder.imageView.layoutParams as RelativeLayout.LayoutParams

        dispose(holder.imageView)
        if (node.isFolder) {
            params.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
            params.height = params.width
            val margin = Util.dp2px(Constants.ICON_MARGIN_DP.toFloat())
            params.setMargins(margin, margin, margin, margin)

            holder.imageView.setImageResource(IconPackR.drawable.ic_folder_medium_solid)
            holder.textViewFileSize.text = MegaApiUtils.getMegaNodeFolderInfo(node, context)

            if (node.isInShare) {
                val sharesIncoming = megaApi.inSharesList
                for (j in sharesIncoming.indices) {
                    val mS = sharesIncoming[j]
                    if (mS.nodeHandle == node.handle) {
                        val user = megaApi.getContact(mS.user)
                        if (user != null) {
                            holder.textViewFileSize.text = ContactUtil.getMegaUserNameDB(user)
                        } else {
                            holder.textViewFileSize.text = mS.user
                        }
                    }
                }

                holder.permissionsIcon.visibility = View.VISIBLE
                val accessLevel = megaApi.getAccess(node)

                if (accessLevel == MegaShare.ACCESS_FULL) {
                    holder.permissionsIcon.setImageResource(R.drawable.ic_shared_fullaccess)
                } else if (accessLevel == MegaShare.ACCESS_READ) {
                    holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read)
                } else {
                    holder.permissionsIcon.setImageResource(R.drawable.ic_shared_read_write)
                }
            } else {
                holder.permissionsIcon.visibility = View.GONE
                holder.textViewFileSize.text = MegaApiUtils.getMegaNodeFolderInfo(node, context)
            }

            if (isMultipleSelect() && isItemChecked(position)) {
                holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder)
            } else {
                holder.imageView.setImageResource(
                    if (node.isInShare)
                        IconPackR.drawable.ic_folder_incoming_medium_solid
                    else
                        IconPackR.drawable.ic_folder_medium_solid
                )
            }
        } else {
            holder.permissionsIcon.visibility = View.GONE

            holder.textViewFileSize.text = getFileInfo(node, context)

            if (isMultipleSelect() && isItemChecked(position)) {
                params.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
                params.height = params.width
                params.setMargins(Util.dp2px(Constants.ICON_MARGIN_DP.toFloat()), 0, 0, 0)
                holder.imageView.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder)
            } else {
                val margin: Int
                if (node.hasThumbnail()) {
                    params.width = Util.dp2px(Constants.THUMB_SIZE_DP.toFloat())
                    params.height = params.width
                    margin = Util.dp2px(Constants.THUMB_MARGIN_DP.toFloat())
                    val placeholder = ContextCompat.getDrawable(
                        context,
                        typeForName(node.name).iconResourceId
                    )?.asImage()
                    val imageRequest = ImageRequest.Builder(context)
                        .data(fromHandle(node.handle))
                        .placeholder(placeholder)
                        .size(params.width, params.height)
                        .scale(Scale.FILL)
                        .transformations(
                            RoundedCornersTransformation(
                                context
                                    .resources
                                    .getDimension(R.dimen.thumbnail_corner_radius)
                            )
                        )
                        .target { image ->
                            holder.imageView.setImageDrawable(image.asDrawable(context.resources))
                        }
                        .crossfade(true)
                        .build()

                    SingletonImageLoader.get(context).enqueue(imageRequest)
                } else {
                    params.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
                    params.height = params.width
                    margin = Util.dp2px(Constants.ICON_MARGIN_DP.toFloat())
                    holder.imageView.setImageResource(typeForName(node.name).iconResourceId)
                }

                params.setMargins(margin, margin, margin, margin)
            }
        }

        holder.imageView.layoutParams = params
    }

    override fun onClick(v: View) {
        Timber.d("onClick")
        val holder = v.tag as ViewHolderProvider

        val currentPosition = holder.currentPosition

        val id = v.id
        if (id == R.id.file_explorer_filename || id == R.id.file_explorer_item_layout) {
            if (fragment is CloudDriveProviderFragment) {
                (fragment).itemClick(currentPosition)
            } else if (fragment is IncomingSharesProviderFragment) {
                (fragment).itemClick(currentPosition)
            }
        }
    }

    fun isMultipleSelect(): Boolean {
        return isMultipleSelect
    }

    fun setNodes(nodes: ArrayList<MegaNode>?) {
        Timber.d("Setting nodes: ${nodes?.size}")
        this.nodes = nodes
        positionClicked = -1
        notifyDataSetChanged()
    }

    fun setMultipleSelect(multipleSelect: Boolean) {
        Timber.d("multipleSelect: %s", multipleSelect)
        if (this.isMultipleSelect != multipleSelect) {
            this.isMultipleSelect = multipleSelect
        }
        if (this.isMultipleSelect) {
            selectedItems = SparseBooleanArray()
        }
    }

    fun toggleAllSelection(pos: Int) {
        Timber.d("pos: %s", pos)
        val positionToflip = pos

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos)
            selectedItems.delete(pos)
        } else {
            Timber.d("PUT pos: %s", pos)
            selectedItems.put(pos, true)
        }

        val view = listView.findViewHolderForLayoutPosition(pos) as ViewHolderProvider?
        if (view != null) {
            Timber.d("Start animation: %d multiselection state: %s", pos, isMultipleSelect())
            val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
            flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    if (selectedItems.size <= 0) {
                        if (type == Constants.INCOMING_SHARES_PROVIDER_ADAPTER) {
                            (fragment as? IncomingSharesProviderFragment)?.hideMultipleSelect()
                        } else {
                            (fragment as? CloudDriveProviderFragment)?.hideMultipleSelect()
                        }
                    }
                    Timber.d("Notified item changed")
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

    fun toggleSelection(position: Int) {
        Timber.d("position: %s", position)

        if (selectedItems.get(position, false)) {
            Timber.d("Delete pos: %s", position)
            selectedItems.delete(position)
        } else {
            Timber.d("PUT pos: %s", position)
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)

        val view = listView.findViewHolderForLayoutPosition(position) as? ViewHolderProvider?
        if (view != null) {
            Timber.d("Start animation: %s", position)
            val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
            flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    if (selectedItems.size <= 0) {
                        if (type == Constants.INCOMING_SHARES_PROVIDER_ADAPTER) {
                            (fragment as? IncomingSharesProviderFragment)?.hideMultipleSelect()
                        } else {
                            (fragment as? CloudDriveProviderFragment)?.hideMultipleSelect()
                        }
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

    val selectedNodes: MutableList<MegaNode?>
        get() {
            val nodes = ArrayList<MegaNode?>()

            for (i in 0..<selectedItems.size) {
                if (selectedItems.valueAt(i) == true) {
                    val document = getItem(selectedItems.keyAt(i))
                    if (document != null) {
                        nodes.add(document)
                    }
                }
            }

            return nodes
        }

    fun getSelectedItems(): MutableList<Int?> {
        val items: MutableList<Int?> = ArrayList<Int?>(selectedItems.size)
        for (i in 0..<selectedItems.size) {
            items.add(selectedItems.keyAt(i))
        }

        return items
    }

    fun selectAll() {
        for (i in 0..<this.itemCount) {
            if (!isItemChecked(i)) {
                toggleAllSelection(i)
            }
        }
    }

    fun clearSelections() {
        Timber.d("clearSelections")
        for (i in 0..<this.itemCount) {
            if (isItemChecked(i)) {
                toggleAllSelection(i)
            }
        }
    }

    private fun isItemChecked(position: Int): Boolean {
        return selectedItems.get(position)
    }

    override fun onLongClick(view: View): Boolean {
        Timber.d("OnLongClick")

        val holder = view.tag as ViewHolderProvider
        val currentPosition = holder.adapterPosition
        val success = when (type) {
            Constants.INCOMING_SHARES_PROVIDER_ADAPTER ->
                (fragment as? IncomingSharesProviderFragment)?.apply {
                    activateActionMode()
                    itemClick(currentPosition)
                }

            else ->
                (fragment as? CloudDriveProviderFragment)?.apply {
                    activateActionMode()
                    itemClick(currentPosition)
                }
        } != null

        return success
    }
}
