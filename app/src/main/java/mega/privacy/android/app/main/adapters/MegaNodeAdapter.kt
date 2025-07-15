package mega.privacy.android.app.main.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.SparseBooleanArray
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.request.target
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import coil3.util.CoilUtils
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.dragger.DragThumbnailGetter
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.di.getDbHandler
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel.Companion.orderNameMap
import mega.privacy.android.app.main.ContactFileListActivity
import mega.privacy.android.app.main.ContactFileListFragment
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.adapters.MegaNodeAdapter.ViewHolderBrowser
import mega.privacy.android.app.main.contactSharedFolder.ContactSharedFolderFragment
import mega.privacy.android.app.presentation.backups.BackupsFragment
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetDialogFragment
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.getFolderIcon
import mega.privacy.android.app.utils.MegaNodeUtil.getNodeLabelDrawable
import mega.privacy.android.app.utils.MegaNodeUtil.getNumberOfFolders
import mega.privacy.android.app.utils.MegaNodeUtil.showTakenDownDialog
import mega.privacy.android.app.utils.NodeTakenDownDialogListener
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest.Companion.fromHandle
import mega.privacy.android.icon.pack.R as IconPackR
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber
import java.util.stream.Collectors

class MegaNodeAdapter : RecyclerView.Adapter<ViewHolderBrowser?>,
    View.OnClickListener, OnLongClickListener, SectionTitleProvider, RotatableAdapter,
    NodeTakenDownDialogListener, DragThumbnailGetter {
    private var context: Context? = null
    private var megaApi: MegaApiAndroid? = null

    private var nodes: MutableList<MegaNode?>? = null

    /**
     * List of shareData associated to the List of MegaNode
     * This list is used to carry additional information for incoming shares and outgoing shares
     * Each ShareData element at a specific position is associated to the MegaNode element
     * at the same position of the nodes attributes
     * The element is null if the node associated is already verified
     */
    private var shareData: MutableList<ShareData?>? = null

    private var fragment: Any? = null
    var parentHandle: Long = -1
    private var outMetrics: DisplayMetrics? = null

    private var placeholderCount = 0

    private val selectedItems = SparseBooleanArray()

    /**
     * the flag to store the node position where still remained unhandled
     */
    private var unHandledItem = -1

    /**
     * the dialog to show taken down message
     */
    private var takenDownDialog: AlertDialog? = null

    private var listFragment: RecyclerView? = null
    private var dbH: DatabaseHandler? = null
    private var multipleSelect = false
    private var type = Constants.FILE_BROWSER_ADAPTER
    var adapterType = 0

    private var isContactVerificationOn = false

    private var sortByViewModel: SortByHeaderViewModel? = null

    open class ViewHolderBrowser(v: View) : RecyclerView.ViewHolder(v) {
        var savedOffline: ImageView? = null
        var publicLinkImage: ImageView? = null
        var takenDownImage: ImageView? = null
        var textViewFileName: TextView? = null
        var imageFavourite: ImageView? = null
        var imageLabel: ImageView? = null
        var textViewFileSize: EmojiTextView? = null
        var document: Long = 0
        var itemLayout: RelativeLayout? = null
    }

    class ViewHolderBrowserList(v: View) : ViewHolderBrowser(v) {
        var imageView: ImageView? = null
        var permissionsIcon: ImageView? = null
        var versionsIcon: ImageView? = null
        var threeDotsLayout: RelativeLayout? = null
    }

    class ViewHolderBrowserGrid(v: View) : ViewHolderBrowser(v) {
        var imageViewThumb: ImageView? = null
        var imageViewIcon: ImageView? = null
        var thumbLayout: ConstraintLayout? = null
        var imageViewVideoIcon: ImageView? = null
        var videoDuration: TextView? = null
        var videoInfoLayout: RelativeLayout? = null
        var bottomContainer: ConstraintLayout? = null
        var imageButtonThreeDots: ImageButton? = null

        var folderLayout: View? = null
        var fileLayout: View? = null
        var thumbLayoutForFile: RelativeLayout? = null
        var fileGridIconForFile: ImageView? = null
        var imageButtonThreeDotsForFile: ImageButton? = null
        var textViewFileNameForFile: TextView? = null
        var takenDownImageForFile: ImageView? = null
        var fileGridSelected: ImageView? = null
        var folderGridSelected: ImageView? = null
    }

    inner class ViewHolderSortBy(private val binding: SortByHeaderBinding) :
        ViewHolderBrowser(binding.root) {

        init {
            binding.sortByLayout.setOnClickListener {
                sortByViewModel?.showSortByDialog()
            }

            binding.listModeSwitch.setOnClickListener {
                sortByViewModel?.switchViewType()
            }
        }

        fun bind() {
            var orderType = sortByViewModel?.order?.cloudSortOrder

            // Root of incoming shares tab, display sort options OTHERS
            if (type == Constants.INCOMING_SHARES_ADAPTER
                && (context as ManagerActivity).deepBrowserTreeIncoming == 0
            ) {
                orderType = sortByViewModel?.order?.othersSortOrder
            } else if (type == Constants.OUTGOING_SHARES_ADAPTER
                && (context as ManagerActivity).deepBrowserTreeOutgoing == 0
            ) {
                orderType = sortByViewModel?.order?.othersSortOrder
            }

            orderNameMap[orderType]?.let {
                binding.sortedBy.text = binding.root.context.getString(it)
            }

            if (type == Constants.FOLDER_LINK_ADAPTER) {
                binding.sortByLayout.visibility = View.GONE
            } else {
                binding.sortByLayout.visibility = View.VISIBLE
            }

            binding.listModeSwitch.visibility = if (type == Constants.LINKS_ADAPTER)
                View.GONE
            else
                View.VISIBLE

            binding.listModeSwitch.setImageResource(
                if (sortByViewModel?.isListView() == true)
                    IconPackR.drawable.ic_grid_4_small_thin_outline
                else
                    IconPackR.drawable.ic_list_small_small_thin_outline
            )
        }
    }

    override fun getNodePosition(handle: Long): Int {
        nodes?.let {
            for (i in it.indices) {
                val node = it[i]
                if (node != null && node.handle == handle) {
                    return i
                }
            }
        }

        return Constants.INVALID_POSITION
    }

    override fun getThumbnail(viewHolder: RecyclerView.ViewHolder): View? {
        if (viewHolder is ViewHolderBrowserList) {
            return viewHolder.imageView
        } else if (viewHolder is ViewHolderBrowserGrid) {
            return viewHolder.imageViewThumb
        }

        return null
    }

    override fun getPlaceholderCount(): Int = placeholderCount

    override fun getUnhandledItem(): Int = unHandledItem

    fun toggleSelection(pos: Int) {
        Timber.d("Position: %s", pos)
        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos)
            selectedItems.delete(pos)
        } else {
            Timber.d("PUT pos: %s", pos)
            selectedItems.put(pos, true)
        }

        hideMultipleSelect()
        notifyItemChanged(pos)
    }

    fun startAnimation(pos: Int, delete: Boolean) {
        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            Timber.d("Adapter type is LIST")
            val view = listFragment?.findViewHolderForLayoutPosition(pos) as ViewHolderBrowserList?
            if (view != null) {
                Timber.d("Start animation: %s", pos)
                val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
                flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        if (!delete) {
                            notifyItemChanged(pos)
                        }
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        hideMultipleSelect()
                        if (delete) {
                            notifyItemChanged(pos)
                        }
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })
                view.imageView?.startAnimation(flipAnimation)
            } else {
                Timber.d("View is null - not animation")
                hideMultipleSelect()
                notifyItemChanged(pos)
            }
        } else {
            Timber.d("Adapter type is GRID")
            val node = getItem(pos)
            var isFile = false
            if (node != null) {
                isFile = !node.isFolder
            }
            val view = listFragment?.findViewHolderForLayoutPosition(pos) as ViewHolderBrowserGrid?
            if (view != null) {
                Timber.d("Start animation: %s", pos)
                val flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
                if (!delete && isFile) {
                    notifyItemChanged(pos)
                    flipAnimation.duration = 250
                }
                flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        if (!delete) {
                            notifyItemChanged(pos)
                        }
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        hideMultipleSelect()
                        notifyItemChanged(pos)
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                    }
                })
                if (isFile) {
                    view.fileGridSelected?.startAnimation(flipAnimation)
                } else {
                    view.imageViewIcon?.startAnimation(flipAnimation)
                }
            } else {
                Timber.d("View is null - not animation")
                hideMultipleSelect()
                notifyItemChanged(pos)
            }
        }
    }

    fun hideMultipleSelect() {
        if (selectedItems.size() <= 0) {
            if (type == Constants.BACKUPS_ADAPTER) {
                (fragment as BackupsFragment).hideMultipleSelect()
            } else if (type == Constants.CONTACT_FILE_ADAPTER) {
                (fragment as ContactFileListFragment).hideMultipleSelect()
            } else if (type == Constants.CONTACT_SHARED_FOLDER_ADAPTER) {
                (fragment as ContactSharedFolderFragment).hideMultipleSelect()
            }
        }
    }

    fun selectAll() {
        nodes?.indices?.let {
            for (i in it) {
                selectedItems.put(i, true)
                notifyItemChanged(i)
            }
        }
    }

    fun clearSelections() {
        Timber.d("clearSelections")
        if (nodes == null) {
            return
        }

        nodes?.indices?.let {
            for (i in it) {
                selectedItems.delete(i)
                notifyItemChanged(i)
            }
        }
    }

    private fun isItemChecked(position: Int): Boolean = selectedItems.get(position)

    override fun getSelectedItems(): MutableList<Int?> {
        val items: MutableList<Int?> = ArrayList<Int?>(selectedItems.size())
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    /*
     * Get list of all selected nodes
     */
    val selectedNodes: MutableList<MegaNode?>
        get() {
            val nodes = ArrayList<MegaNode?>()
            for (i in 0 until selectedItems.size()) {
                if (selectedItems.valueAt(i) == true) {
                    val document = getNodeAt(selectedItems.keyAt(i))
                    if (document != null) {
                        nodes.add(document)
                    }
                }
            }
            return nodes
        }

    /*
     * The method to return how many folders in this adapter
     */
    override fun getFolderCount(): Int = getNumberOfFolders(nodes)

    /**
     * In grid view.
     * For folder count is odd. Insert null element as placeholder.
     *
     * @param nodes Origin nodes to show.
     * @return Nodes list with placeholder.
     */
    private fun insertPlaceHolderNode(nodes: MutableList<MegaNode?>): MutableList<MegaNode?> {
        if (adapterType == ITEM_VIEW_TYPE_LIST) {
            if (shouldShowSortByHeader(nodes)) {
                placeholderCount = 1
                nodes.add(0, null)
            } else {
                placeholderCount = 0
            }

            return nodes
        }

        val folderCount = getNumberOfFolders(nodes)
        var spanCount = 2

        if (listFragment is NewGridRecyclerView) {
            spanCount = (listFragment as NewGridRecyclerView).spanCount
        }

        placeholderCount =
            if ((folderCount % spanCount) == 0) 0 else spanCount - (folderCount % spanCount)

        if (folderCount > 0 && placeholderCount != 0 && adapterType == ITEM_VIEW_TYPE_GRID) {
            //Add placeholder at folders' end.
            for (i in 0 until placeholderCount) {
                try {
                    nodes.add(folderCount + i, null)
                } catch (e: IndexOutOfBoundsException) {
                    Timber.e(
                        e,
                        "Inserting placeholders [nodes.size]: %d [folderCount+i]: %d",
                        nodes.size,
                        folderCount + i
                    )
                }
            }
        }

        if (shouldShowSortByHeader(nodes)) {
            placeholderCount++
            nodes.add(0, null)
        }

        return nodes
    }

    /**
     * Checks if should show sort by header.
     * It should show the header if the list of nodes is not empty and if the adapter is not:
     * CONTACT_SHARED_FOLDER_ADAPTER or CONTACT_FILE_ADAPTER.
     *
     * @param nodes List of nodes to check if is empty or not.
     * @return True if should show the sort by header, false otherwise.
     */
    private fun shouldShowSortByHeader(nodes: MutableList<MegaNode?>): Boolean {
        return !nodes.isEmpty() && type != Constants.CONTACT_SHARED_FOLDER_ADAPTER && type != Constants.CONTACT_FILE_ADAPTER
    }

    fun getSpanSizeLookup(spanCount: Int): GridLayoutManager.SpanSizeLookup {
        return (object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (getItemViewType(position) == ITEM_VIEW_TYPE_HEADER) spanCount else 1
            }
        }) as GridLayoutManager.SpanSizeLookup
    }

    constructor(
        context: Context, fragment: Any, nodes: MutableList<MegaNode?>?,
        parentHandle: Long, recyclerView: RecyclerView, type: Int, adapterType: Int,
    ) {
        initAdapter(context, fragment, nodes, parentHandle, recyclerView, type, adapterType)
    }

    constructor(
        context: Context, fragment: Any, nodes: MutableList<MegaNode?>?,
        parentHandle: Long, recyclerView: RecyclerView, type: Int, adapterType: Int,
        sortByHeaderViewModel: SortByHeaderViewModel,
    ) {
        initAdapter(context, fragment, nodes, parentHandle, recyclerView, type, adapterType)
        this.sortByViewModel = sortByHeaderViewModel
    }

    /**
     * Initializes the principal properties of the adapter.
     *
     * @param context      Current Context.
     * @param fragment     Current Fragment.
     * @param nodes        List of nodes.
     * @param parentHandle Current parent handle.
     * @param recyclerView View in which the adapter will be set.
     * @param type         Fragment adapter type.
     * @param adapterType  List or grid adapter type.
     */
    private fun initAdapter(
        context: Context, fragment: Any, nodes: MutableList<MegaNode?>?,
        parentHandle: Long, recyclerView: RecyclerView, type: Int, adapterType: Int,
    ) {
        this.context = context
        this.nodes = nodes
        this.parentHandle = parentHandle
        this.type = type
        this.adapterType = adapterType
        this.fragment = fragment

        dbH = getDbHandler()

        when (type) {
            Constants.CONTACT_FILE_ADAPTER -> {
                (context as ContactFileListActivity).setParentHandle(parentHandle)
            }

            Constants.FOLDER_LINK_ADAPTER -> {
                megaApi = ((context as Activity).application as MegaApplication).megaApiFolder
            }

            Constants.BACKUPS_ADAPTER -> {
                Timber.d("onCreate BACKUPS_ADAPTER")
                (context as ManagerActivity).setParentHandleBackups(parentHandle)
            }

            else -> {}
        }

        this.listFragment = recyclerView

        if (megaApi == null) {
            megaApi = ((context as Activity).application as MegaApplication)
                .megaApi
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setNodes(nodes: MutableList<MegaNode?>) {
        this.nodes = insertPlaceHolderNode(nodes)
        Timber.d("setNodes size: %s", this.nodes?.size)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderBrowser {
        Timber.d("onCreateViewHolder")
        val display = (context as Activity).windowManager.defaultDisplay
        outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        if (viewType == ITEM_VIEW_TYPE_LIST) {
            Timber.d("type: ITEM_VIEW_TYPE_LIST")

            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_list, parent, false)
            val holderList = ViewHolderBrowserList(v)
            holderList.itemLayout = v.findViewById(R.id.file_list_item_layout)
            holderList.imageView = v.findViewById(R.id.file_list_thumbnail)
            holderList.savedOffline = v.findViewById(R.id.file_list_saved_offline)

            holderList.publicLinkImage = v.findViewById(R.id.file_list_public_link)
            holderList.takenDownImage = v.findViewById(R.id.file_list_taken_down)
            holderList.permissionsIcon =
                v.findViewById(R.id.file_list_incoming_permissions)

            holderList.versionsIcon = v.findViewById(R.id.file_list_versions_icon)

            holderList.textViewFileName = v.findViewById(R.id.file_list_filename)

            holderList.imageLabel = v.findViewById(R.id.img_label)
            holderList.imageFavourite = v.findViewById(R.id.img_favourite)

            if (parent.context.resources
                    .configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            ) {
                holderList.textViewFileName?.maxWidth = Util.scaleWidthPx(275, outMetrics)
            } else {
                holderList.textViewFileName?.maxWidth = Util.scaleWidthPx(190, outMetrics)
            }

            holderList.textViewFileSize = v.findViewById(R.id.file_list_filesize)
            if (Util.isScreenInPortrait(context)) {
                holderList.textViewFileSize?.setMaxWidthEmojis(
                    Util.dp2px(
                        Constants.MAX_WIDTH_CONTACT_NAME_PORT.toFloat(),
                        outMetrics
                    )
                )
            } else {
                holderList.textViewFileSize?.setMaxWidthEmojis(
                    Util.dp2px(
                        Constants.MAX_WIDTH_CONTACT_NAME_LAND.toFloat(),
                        outMetrics
                    )
                )
            }

            holderList.threeDotsLayout =
                v.findViewById(R.id.file_list_three_dots_layout)

            holderList.savedOffline?.visibility = View.INVISIBLE

            holderList.publicLinkImage?.visibility = View.INVISIBLE

            holderList.takenDownImage?.visibility = View.GONE

            holderList.textViewFileSize?.visibility = View.VISIBLE

            holderList.itemLayout?.tag = holderList
            holderList.itemLayout?.setOnClickListener(this)
            holderList.itemLayout?.setOnLongClickListener(this)

            holderList.threeDotsLayout?.tag = holderList
            holderList.threeDotsLayout?.setOnClickListener(this)

            v.tag = holderList
            return holderList
        } else if (viewType == ITEM_VIEW_TYPE_GRID) {
            Timber.d("type: ITEM_VIEW_TYPE_GRID")

            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_file_grid, parent, false)
            val holderGrid = ViewHolderBrowserGrid(v)

            holderGrid.folderLayout = v.findViewById(R.id.item_file_grid_folder)
            holderGrid.fileLayout = v.findViewById(R.id.item_file_grid_file)
            holderGrid.itemLayout = v.findViewById(R.id.file_grid_item_layout)
            holderGrid.imageViewThumb = v.findViewById(R.id.file_grid_thumbnail)
            holderGrid.imageViewIcon = v.findViewById(R.id.file_grid_icon)
            holderGrid.fileGridIconForFile = v.findViewById(R.id.file_grid_icon_for_file)
            holderGrid.thumbLayout =
                v.findViewById(R.id.file_grid_thumbnail_layout)
            holderGrid.thumbLayoutForFile =
                v.findViewById(R.id.file_grid_thumbnail_layout_for_file)
            holderGrid.textViewFileName = v.findViewById(R.id.file_grid_filename)
            holderGrid.textViewFileNameForFile =
                v.findViewById(R.id.file_grid_filename_for_file)
            holderGrid.imageButtonThreeDotsForFile =
                v.findViewById(R.id.file_grid_three_dots_for_file)
            holderGrid.imageButtonThreeDots = v.findViewById(R.id.file_grid_three_dots)
            holderGrid.takenDownImage = v.findViewById(R.id.file_grid_taken_down)
            holderGrid.takenDownImageForFile =
                v.findViewById(R.id.file_grid_taken_down_for_file)
            holderGrid.imageViewVideoIcon = v.findViewById(R.id.file_grid_video_icon)
            holderGrid.videoDuration = v.findViewById(R.id.file_grid_title_video_duration)
            holderGrid.videoInfoLayout =
                v.findViewById(R.id.item_file_videoinfo_layout)
            holderGrid.fileGridSelected = v.findViewById(R.id.file_grid_check_icon)
            holderGrid.folderGridSelected = v.findViewById(R.id.folder_grid_check_icon)
            holderGrid.bottomContainer =
                v.findViewById(R.id.grid_bottom_container)
            holderGrid.bottomContainer?.tag = holderGrid
            holderGrid.bottomContainer?.setOnClickListener(this)

            if (parent.context.resources
                    .configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            ) {
                holderGrid.textViewFileNameForFile?.maxWidth = Util.scaleWidthPx(70, outMetrics)
            } else {
                holderGrid.textViewFileNameForFile?.maxWidth = Util.scaleWidthPx(140, outMetrics)
            }

            holderGrid.takenDownImage?.visibility = View.GONE
            holderGrid.takenDownImageForFile?.visibility = View.GONE

            holderGrid.itemLayout?.tag = holderGrid
            holderGrid.itemLayout?.setOnClickListener(this)
            holderGrid.itemLayout?.setOnLongClickListener(this)

            holderGrid.imageButtonThreeDots?.tag = holderGrid
            holderGrid.imageButtonThreeDots?.setOnClickListener(this)
            holderGrid.imageButtonThreeDotsForFile?.tag = holderGrid
            holderGrid.imageButtonThreeDotsForFile?.setOnClickListener(this)
            v.tag = holderGrid

            return holderGrid
        } else {
            val binding =
                SortByHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return this.ViewHolderSortBy(binding)
        }
    }

    override fun onBindViewHolder(holder: ViewHolderBrowser, position: Int) {
        Timber.d("Position: %s", position)

        when (getItemViewType(position)) {
            ITEM_VIEW_TYPE_HEADER -> (holder as ViewHolderSortBy).bind()
            ITEM_VIEW_TYPE_LIST -> {
                val holderList = holder as ViewHolderBrowserList
                onBindViewHolderList(holderList, position)
            }

            ITEM_VIEW_TYPE_GRID -> {
                val holderGrid = holder as ViewHolderBrowserGrid
                onBindViewHolderGrid(holderGrid, position)
            }
        }

        reSelectUnhandledNode()
    }

    fun onBindViewHolderGrid(holder: ViewHolderBrowserGrid, position: Int) {
        Timber.d("Position: %s", position)
        val node = getItem(position)
        //Placeholder for folder when folder count is odd.
        if (node == null) {
            holder.folderLayout?.visibility = View.GONE
            holder.fileLayout?.visibility = View.GONE
            holder.itemLayout?.visibility = View.GONE
            return
        }

        holder.document = node.handle
        Timber.d("Node : %d %d", position, node.handle)

        holder.textViewFileName?.text = node.name
        holder.videoInfoLayout?.visibility = View.GONE

        holder.imageViewThumb?.apply { CoilUtils.dispose(this) }
        if (node.isTakenDown) {
            holder.textViewFileNameForFile?.apply {
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.red_800_red_400
                    )
                )
            }
            holder.textViewFileName?.apply {
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.red_800_red_400
                    )
                )
            }
            holder.takenDownImage?.visibility = View.VISIBLE
            holder.takenDownImageForFile?.visibility = View.VISIBLE
        } else {
            holder.textViewFileNameForFile?.apply {
                setTextColor(
                    ColorUtils.getThemeColor(
                        context,
                        android.R.attr.textColorPrimary
                    )
                )
            }
            holder.textViewFileName?.apply {
                setTextColor(
                    ColorUtils.getThemeColor(
                        context,
                        android.R.attr.textColorPrimary
                    )
                )
            }
            holder.takenDownImage?.visibility = View.GONE
            holder.takenDownImageForFile?.visibility = View.GONE
        }

        if (node.isFolder) {
            holder.itemLayout?.visibility = View.VISIBLE
            holder.folderLayout?.visibility = View.VISIBLE
            holder.fileLayout?.visibility = View.GONE

            setFolderGridSelected(holder, position)

            holder.imageViewIcon?.visibility = View.VISIBLE
            holder.imageViewIcon?.setImageResource(
                getFolderIcon(
                    node,
                    if (type == Constants.OUTGOING_SHARES_ADAPTER) DrawerItem.SHARED_ITEMS else DrawerItem.CLOUD_DRIVE
                )
            )
            holder.imageViewThumb?.visibility = View.GONE
            holder.thumbLayout?.setBackgroundColor(Color.TRANSPARENT)
        } else if (node.isFile) {
            holder.itemLayout?.visibility = View.VISIBLE
            holder.folderLayout?.visibility = View.GONE
            holder.imageViewThumb?.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
            holder.imageViewThumb?.visibility = View.GONE
            holder.fileLayout?.visibility = View.VISIBLE
            holder.textViewFileName?.visibility = View.VISIBLE

            holder.textViewFileNameForFile?.text = node.name
            holder.fileGridIconForFile?.visibility = View.VISIBLE
            holder.fileGridIconForFile?.setImageResource(
                MimeTypeThumbnail.typeForName(node.name).getIconResourceId()
            )

            if (FileUtil.isVideoFile(node.name)) {
                holder.videoInfoLayout?.visibility = View.VISIBLE
                holder.videoDuration?.visibility = View.GONE

                val duration = TimeUtils.getVideoDuration(node.duration)
                if (!duration.isEmpty()) {
                    holder.videoDuration?.text = duration
                    holder.videoDuration?.visibility =
                        if (node.duration <= 0) View.GONE else View.VISIBLE
                }
            }

            if (node.hasThumbnail()) {
                holder.imageViewThumb?.apply {
                    visibility = View.VISIBLE
                    val imageRequest = ImageRequest.Builder(context)
                        .data(fromHandle(node.handle))
                        .target(this)
                        .transformations(
                            RoundedCornersTransformation(
                                context.resources.getDimension(R.dimen.thumbnail_corner_radius)
                            )
                        )
                        .crossfade(true)
                        .listener(
                            object : ImageRequest.Listener {
                                override fun onSuccess(
                                    request: ImageRequest,
                                    result: SuccessResult,
                                ) {
                                    super.onSuccess(request, result)
                                    holder.fileGridIconForFile?.visibility = View.GONE
                                }

                            })
                        .build()

                    SingletonImageLoader
                        .get(context)
                        .enqueue(imageRequest)
                }
            }

            if (isMultipleSelect) {
                holder.imageButtonThreeDotsForFile?.visibility = View.GONE
                holder.fileGridSelected?.visibility =
                    if (isItemChecked(position)) View.VISIBLE else View.INVISIBLE
            } else {
                holder.fileGridSelected?.visibility = View.GONE
                holder.imageButtonThreeDotsForFile?.visibility = View.VISIBLE
            }

            holder.textViewFileName?.apply {
                background = ContextCompat.getDrawable(
                    context,
                    if (isMultipleSelect && isItemChecked(position)) R.drawable.background_item_grid_selected else R.drawable.background_item_grid
                )
            }
        }
    }

    private fun setFolderGridSelected(holder: ViewHolderBrowserGrid, position: Int) {
        if (isMultipleSelect) {
            holder.imageButtonThreeDots?.visibility = View.GONE
            holder.folderGridSelected?.visibility =
                if (isItemChecked(position)) View.VISIBLE else View.INVISIBLE
        } else {
            holder.folderGridSelected?.visibility = View.GONE
            holder.imageButtonThreeDots?.visibility = View.VISIBLE
        }

        holder.itemLayout?.apply {
            background = ContextCompat.getDrawable(
                context,
                if (isMultipleSelect && isItemChecked(position)) R.drawable.background_item_grid_selected else R.drawable.background_item_grid
            )
        }
    }

    private fun setFolderListSelected(
        holder: ViewHolderBrowserList,
        position: Int,
        folderDrawableResId: Int,
    ) {
        if (isMultipleSelect && isItemChecked(position)) {
            val paramsMultiselect =
                holder.imageView?.layoutParams as RelativeLayout.LayoutParams?
            paramsMultiselect?.height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                holder.imageView?.resources?.displayMetrics
            ).toInt()
            paramsMultiselect?.width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                holder.imageView?.resources?.displayMetrics
            ).toInt()
            paramsMultiselect?.setMargins(0, 0, 0, 0)
            holder.imageView?.layoutParams = paramsMultiselect
            holder.imageView?.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder)
        } else {
            holder.itemLayout?.background = null
            holder.imageView?.setImageResource(folderDrawableResId)
        }
    }

    fun onBindViewHolderList(holder: ViewHolderBrowserList, position: Int) {
        Timber.d("Position: %s", position)

        holder.textViewFileSize?.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        val node = getItem(position)
        if (node == null) {
            return
        }
        holder.document = node.handle

        holder.textViewFileName?.text = node.name
        holder.textViewFileSize?.text = ""

        holder.imageFavourite?.visibility =
            if (type != Constants.INCOMING_SHARES_ADAPTER && type != Constants.FOLDER_LINK_ADAPTER && node.isFavourite) View.VISIBLE else View.GONE

        if (type != Constants.FOLDER_LINK_ADAPTER && node.label != MegaNode.NODE_LBL_UNKNOWN) {
            val drawable = getNodeLabelDrawable(node.label, holder.itemView.resources)
            holder.imageLabel?.setImageDrawable(drawable)
            holder.imageLabel?.visibility = View.VISIBLE
        } else {
            holder.imageLabel?.visibility = View.GONE
        }

        holder.publicLinkImage?.visibility = View.INVISIBLE
        holder.permissionsIcon?.visibility = View.GONE

        if (node.isExported && type != Constants.LINKS_ADAPTER) {
            //Node has public link
            holder.publicLinkImage?.visibility = View.VISIBLE
            if (node.isExpired) {
                Timber.w("Node exported but expired!!")
            }
        } else {
            holder.publicLinkImage?.visibility = View.INVISIBLE
        }

        if (node.isTakenDown) {
            holder.textViewFileName?.apply {
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.red_800_red_400
                    )
                )
            }
            holder.takenDownImage?.visibility = View.VISIBLE
        } else {
            holder.textViewFileName?.apply {
                setTextColor(
                    ColorUtils.getThemeColor(
                        context,
                        android.R.attr.textColorPrimary
                    )
                )
            }
            holder.takenDownImage?.visibility = View.GONE
        }

        holder.imageView?.visibility = View.VISIBLE
        holder.imageView?.let { CoilUtils.dispose(it) }
        if (node.isFolder) {
            Timber.d("Node is folder")
            holder.itemLayout?.background = null
            val params = holder.imageView?.layoutParams as RelativeLayout.LayoutParams?
            params?.height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                holder.imageView?.resources?.displayMetrics
            ).toInt()
            params?.width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                holder.imageView?.resources?.displayMetrics
            ).toInt()
            params?.setMargins(0, 0, 0, 0)
            holder.imageView?.layoutParams = params

            holder.textViewFileSize?.visibility = View.VISIBLE
            holder.textViewFileSize?.text = if (type == Constants.FOLDER_LINK_ADAPTER)
                MegaApiUtils.getMegaNodeFolderLinkInfo(node, context)
            else
                MegaApiUtils.getMegaNodeFolderInfo(node, context)
            holder.versionsIcon?.visibility = View.GONE

            setFolderListSelected(
                holder,
                position,
                getFolderIcon(
                    node,
                    if (type == Constants.OUTGOING_SHARES_ADAPTER) DrawerItem.SHARED_ITEMS else DrawerItem.CLOUD_DRIVE
                )
            )
            if (isMultipleSelect) {
                holder.threeDotsLayout?.visibility = View.INVISIBLE
            } else {
                holder.threeDotsLayout?.visibility = View.VISIBLE
            }
            if (type == Constants.CONTACT_FILE_ADAPTER || type == Constants.CONTACT_SHARED_FOLDER_ADAPTER) {
                val firstLevel: Boolean = if (type == Constants.CONTACT_FILE_ADAPTER) {
                    (fragment as ContactFileListFragment).isEmptyParentHandleStack
                } else {
                    true
                }

                if (firstLevel) {
                    val accessLevel = megaApi?.getAccess(node)

                    if (accessLevel == MegaShare.ACCESS_FULL) {
                        holder.permissionsIcon?.setImageResource(R.drawable.ic_shared_fullaccess)
                    } else if (accessLevel == MegaShare.ACCESS_READWRITE) {
                        holder.permissionsIcon?.setImageResource(R.drawable.ic_shared_read_write)
                    } else {
                        holder.permissionsIcon?.setImageResource(R.drawable.ic_shared_read)
                    }
                    holder.permissionsIcon?.visibility = View.VISIBLE
                } else {
                    holder.permissionsIcon?.visibility = View.GONE
                }
            } else if (type == Constants.INCOMING_SHARES_ADAPTER) {
                holder.publicLinkImage?.visibility = View.INVISIBLE

                if (node.isTakenDown) {
                    holder.textViewFileName?.apply {
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                R.color.red_800_red_400
                            )
                        )
                    }
                    holder.takenDownImage?.visibility = View.VISIBLE
                } else {
                    holder.textViewFileName?.apply {
                        setTextColor(
                            ColorUtils.getThemeColor(
                                context,
                                android.R.attr.textColorPrimary
                            )
                        )
                    }
                    holder.takenDownImage?.visibility = View.GONE
                }

                //Show the owner of the shared folder
                val sharesIncoming = megaApi?.inSharesList
                sharesIncoming?.let {
                    for (j in sharesIncoming.indices) {
                        val mS = sharesIncoming[j]
                        if (mS.nodeHandle == node.handle) {
                            val user = megaApi?.getContact(mS.user)
                            val isContactVerifiedByMega = megaApi?.areCredentialsVerified(user)
                            if (user != null) {
                                holder.textViewFileSize?.text = ContactUtil.getMegaUserNameDB(user)
                            } else {
                                holder.textViewFileSize?.text = mS.user
                            }
                            if (isContactVerificationOn && isContactVerifiedByMega == true) {
                                holder.textViewFileSize?.setCompoundDrawablesWithIntrinsicBounds(
                                    0,
                                    0,
                                    mega.privacy.android.icon.pack.R.drawable.ic_contact_verified,
                                    0
                                )
                            } else {
                                holder.textViewFileSize?.setCompoundDrawablesWithIntrinsicBounds(
                                    0,
                                    0,
                                    0,
                                    0
                                )
                            }
                        }
                    }
                }
                if ((context as ManagerActivity).deepBrowserTreeIncoming == 0) {
                    val accessLevel = megaApi?.getAccess(node)

                    if (accessLevel == MegaShare.ACCESS_FULL) {
                        holder.permissionsIcon?.setImageResource(R.drawable.ic_shared_fullaccess)
                    } else if (accessLevel == MegaShare.ACCESS_READWRITE) {
                        holder.permissionsIcon?.setImageResource(R.drawable.ic_shared_read_write)
                    } else {
                        holder.permissionsIcon?.setImageResource(R.drawable.ic_shared_read)
                    }
                    val hasUnverifiedNodes = shareData != null && shareData?.get(position) != null
                    if (hasUnverifiedNodes) {
                        showUnverifiedNodeUi(holder, true, node, null)
                    }
                    holder.permissionsIcon?.visibility = View.VISIBLE
                } else {
                    holder.permissionsIcon?.visibility = View.GONE
                }
            } else if (type == Constants.OUTGOING_SHARES_ADAPTER) {
                //Show the number of contacts who shared the folder if more than one contact and name of contact if that is not the case
                holder.textViewFileSize?.apply {
                    text = getOutgoingSubtitle(
                        holder.textViewFileSize?.getText().toString(), node, context
                    )
                }
                val hasUnverifiedNodes = shareData != null && shareData?.get(position) != null
                if (hasUnverifiedNodes) {
                    showUnverifiedNodeUi(holder, false, node, shareData?.get(position))
                }
            }
        } else {
            Timber.d("Node is file")
            val isLinksRoot =
                type == Constants.LINKS_ADAPTER && (context as ManagerActivity).getHandleFromLinksViewModel() == -1L
            holder.textViewFileSize?.text = TextUtil.getFileInfo(
                Util.getSizeString(node.size, context),
                TimeUtils.formatLongDateTime(if (isLinksRoot) node.publicLinkCreationTime else node.modificationTime)
            )

            if (megaApi?.hasVersions(node) == true) {
                holder.versionsIcon?.visibility = View.VISIBLE
            } else {
                holder.versionsIcon?.visibility = View.GONE
            }

            val params = holder.imageView?.layoutParams as RelativeLayout.LayoutParams?
            params?.height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                holder.imageView?.resources?.displayMetrics
            ).toInt()
            params?.width = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                48f,
                holder.imageView?.resources?.displayMetrics
            ).toInt()
            params?.setMargins(0, 0, 0, 0)
            holder.imageView?.layoutParams = params

            if (!isMultipleSelect) {
                Timber.d("Not multiselect")
                holder.itemLayout?.background = null

                Timber.d("Check the thumb")

                if (node.hasThumbnail()) {
                    Timber.d("Node has thumbnail")
                    holder.imageView?.let {
                        loadThumbnail(node, it)
                    }
                } else {
                    Timber.d("Node NOT thumbnail")
                    holder.imageView?.setImageResource(typeForName(node.name).iconResourceId)
                }
                holder.threeDotsLayout?.visibility = View.VISIBLE
            } else {
                Timber.d("Multiselect ON")
                if (this.isItemChecked(position)) {
                    holder.imageView?.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder)
                } else {
                    holder.itemLayout?.background = null
                    Timber.d("Check the thumb")

                    if (node.hasThumbnail()) {
                        Timber.d("Node has thumbnail")
                        holder.imageView?.let {
                            loadThumbnail(node, it)
                        }
                    } else {
                        Timber.d("Node NOT thumbnail")
                        holder.imageView?.setImageResource(typeForName(node.name).iconResourceId)
                    }
                }
                holder.threeDotsLayout?.visibility = View.INVISIBLE
            }
        }

        //Check if is an offline file to show the red arrow
        if (OfflineUtils.availableOffline(context, node)) {
            holder.savedOffline?.visibility = View.VISIBLE
        } else {
            holder.savedOffline?.visibility = View.INVISIBLE
        }
    }

    private fun loadThumbnail(node: MegaNode, target: ImageView) {
        target.apply {
            val iconRes = typeForName(node.name).iconResourceId
            val placeholder = ContextCompat.getDrawable(context, iconRes)?.asImage()
            val imageRequest = ImageRequest.Builder(context)
                .placeholder(placeholder)
                .data(fromHandle(node.handle))
                .target(this)
                .crossfade(true)
                .transformations(
                    RoundedCornersTransformation(
                        context.resources.getDimension(R.dimen.thumbnail_corner_radius)
                    )
                )
                .listener(object : ImageRequest.Listener {
                    override fun onSuccess(request: ImageRequest, result: SuccessResult) {
                        val params = target.layoutParams as RelativeLayout.LayoutParams
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
                            6f,
                            context.resources.displayMetrics
                        ).toInt()
                        params.setMargins(left, 0, 0, 0)

                        target.layoutParams = params
                    }
                })
                .build()

            SingletonImageLoader.get(context).enqueue(imageRequest)
        }
    }

    private fun getItemNode(position: Int): String? {
        if (nodes?.get(position) != null) {
            return nodes?.get(position)?.name
        }
        return null
    }


    override fun getItemCount(): Int {
        return nodes?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (nodes?.isEmpty() == false && position == 0
            && type != Constants.CONTACT_SHARED_FOLDER_ADAPTER
            && type != Constants.CONTACT_FILE_ADAPTER
        )
            ITEM_VIEW_TYPE_HEADER
        else
            adapterType
    }

    fun getItem(position: Int): MegaNode? {
        return nodes?.get(position)
    }

    override fun getSectionTitle(position: Int, context: Context?): String? {
        if (getItemNode(position) != null && getItemNode(position) != "") {
            return getItemNode(position)?.substring(0, 1)
        }
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onClick(v: View) {
        Timber.d("onClick")

        val holder = v.tag as ViewHolderBrowser
        val currentPosition = holder.adapterPosition

        Timber.d("Current position: %s", currentPosition)

        if (currentPosition < 0) {
            Timber.e("Current position error - not valid value")
            return
        }

        val n = getItem(currentPosition)
        var sd: ShareData? = null
        if (shareData != null) {
            sd = shareData?.get(currentPosition)
        }
        if (n == null) {
            return
        }

        val id = v.id
        if (id == R.id.grid_bottom_container || id == R.id.file_list_three_dots_layout || id == R.id.file_grid_three_dots || id == R.id.file_grid_three_dots_for_file) {
            threeDotsClicked(currentPosition, n, sd)
        } else if (id == R.id.file_list_item_layout || id == R.id.file_grid_item_layout) {
            if (n.isTakenDown && !isMultipleSelect) {
                takenDownDialog = showTakenDownDialog(n.isFolder, this, v.context)
                unHandledItem = currentPosition
            } else if (n.isFile && !Util.isOnline(context) && FileUtil.getLocalFile(n) == null) {
                if (Util.isOffline(context)) {
                    Timber.d("File is Offline")
                }
            } else {
                fileClicked(currentPosition)
            }
        }
    }

    private fun fileClicked(currentPosition: Int) {
        if (type == Constants.BACKUPS_ADAPTER) {
            (fragment as BackupsFragment).onNodeSelected(currentPosition)
        } else if (type == Constants.CONTACT_FILE_ADAPTER) {
            (fragment as ContactFileListFragment).itemClick(currentPosition)
        } else if (type == Constants.CONTACT_SHARED_FOLDER_ADAPTER) {
            (fragment as ContactSharedFolderFragment).itemClick(currentPosition)
        }
    }

    private fun threeDotsClicked(currentPosition: Int, n: MegaNode, sd: ShareData?) {
        Timber.d("onClick: file_list_three_dots: %s", currentPosition)
        if (Util.isOffline(context)) {
            return
        }

        if (isMultipleSelect) {
            if (type == Constants.BACKUPS_ADAPTER) {
                (fragment as BackupsFragment).onNodeSelected(currentPosition)
            } else if (type == Constants.CONTACT_FILE_ADAPTER) {
                (fragment as ContactFileListFragment).itemClick(currentPosition)
            } else if (type == Constants.CONTACT_SHARED_FOLDER_ADAPTER) {
                (fragment as ContactSharedFolderFragment).itemClick(currentPosition)
            }
        } else {
            if (type == Constants.CONTACT_FILE_ADAPTER) {
                (fragment as ContactFileListFragment).showOptionsPanel(n)
            } else if (type == Constants.CONTACT_SHARED_FOLDER_ADAPTER) {
                (fragment as ContactSharedFolderFragment).showOptionsPanel(n)
            } else {
                (context as ManagerActivity).showNodeOptionsPanel(
                    n,
                    NodeOptionsBottomSheetDialogFragment.DEFAULT_MODE,
                    sd
                )
            }
        }
    }

    override fun onLongClick(view: View): Boolean {
        Timber.d("OnLongCLick")

        if (Util.isOffline(context)) {
            return true
        }

        val holder = view.tag as ViewHolderBrowser
        val currentPosition = holder.adapterPosition
        if (type == Constants.BACKUPS_ADAPTER) {
            (fragment as BackupsFragment).activateActionMode()
            (fragment as BackupsFragment).onNodeSelected(currentPosition)
        } else if (type == Constants.CONTACT_SHARED_FOLDER_ADAPTER) {
            (fragment as ContactSharedFolderFragment).activateActionMode()
            (fragment as ContactSharedFolderFragment).itemClick(currentPosition)
        } else if (type == Constants.CONTACT_FILE_ADAPTER) {
            (fragment as ContactFileListFragment).activateActionMode()
            (fragment as ContactFileListFragment).itemClick(currentPosition)
        }

        return true
    }

    /*
     * Get document at specified position
     */
    private fun getNodeAt(position: Int): MegaNode? {
        return runCatching { nodes?.get(position) }.getOrNull()
    }

    var isMultipleSelect: Boolean
        get() = multipleSelect
        set(value) {
            Timber.d("multipleSelect: %s", value)
            if (this.multipleSelect != value) {
                this.multipleSelect = value
            }
            selectedItems.clear()
        }

    /**
     * Gets the subtitle of a Outgoing item.
     * If it is shared with only one contact it should return the name or email of it.
     * If it is shared with more than one contact it should return the number of contacts.
     * If it is not a root outgoing folder it should return the content of the folder.
     *
     * @param currentSubtitle the current content of the folder (number of files and folders).
     * @param node            outgoing folder.
     * @return the string to show in the subtitle of an outgoing item.
     */
    private fun getOutgoingSubtitle(
        currentSubtitle: String?,
        node: MegaNode?,
        context: Context,
    ): String? {
        var subtitle = currentSubtitle

        // only count the outgoing shares that has been verified
        val sl = megaApi
            ?.getOutShares(node)
            ?.stream()
            ?.filter { obj: MegaShare? -> obj?.isVerified == true }
            ?.collect(Collectors.toList())
        if (sl?.isNotEmpty() == true) {
            if (sl.size == 1 && sl[0]?.user != null) {
                subtitle = sl[0]?.user
                val contact = dbH?.findContactByEmail(subtitle)
                if (contact != null) {
                    val fullName = ContactUtil.getContactNameDB(contact)
                    if (fullName != null) {
                        subtitle = fullName
                    }
                }
            } else {
                subtitle = context.resources
                    .getQuantityString(R.plurals.general_num_shared_with, sl.size, sl.size)
            }
        }

        return subtitle
    }

    /**
     * This is the method to click unhandled taken down dialog again,
     * after the recycler view finish binding adapter
     */
    private fun reSelectUnhandledNode() {
        // if there is no un handled item
        if (unHandledItem == -1) {
            return
        }

        listFragment?.postDelayed(
            Runnable {
                if (takenDownDialog != null && takenDownDialog?.isShowing == true) {
                    return@Runnable
                }
                try {
                    listFragment?.scrollToPosition(unHandledItem)
                    listFragment?.findViewHolderForAdapterPosition(unHandledItem)?.itemView?.performClick()
                } catch (ex: Exception) {
                    Timber.e(ex)
                }
            }, 100
        )
    }

    override fun onDisputeClicked() {
        unHandledItem = -1
    }

    override fun onCancelClicked() {
        unHandledItem = -1
    }

    /**
     * Function to show Unverified node UI items accordingly
     *
     * @param holder         [ViewHolderBrowserList]
     * @param isIncomingNode boolean to indicate if the node is incoming so that
     * "Unencrypted folder" is displayed instead of node name
     */
    private fun showUnverifiedNodeUi(
        holder: ViewHolderBrowserList,
        isIncomingNode: Boolean,
        node: MegaNode,
        shareData: ShareData?,
    ) {
        if (isIncomingNode) {
            if (node.isNodeKeyDecrypted) {
                holder.textViewFileName?.text = node.name
            } else {
                holder.textViewFileName?.text =
                    context?.getString(R.string.shared_items_verify_credentials_undecrypted_folder)
            }
        } else {
            val user = megaApi?.getContact(shareData?.user)
            if (user != null) {
                holder.textViewFileSize?.text = ContactUtil.getMegaUserNameDB(user)
            } else {
                holder.textViewFileSize?.text = shareData?.user
            }
        }
        holder.textViewFileName?.apply {
            setTextColor(
                ContextCompat.getColor(
                    context,
                    R.color.red_600
                )
            )
        }
        holder.permissionsIcon?.visibility = View.VISIBLE
        holder.permissionsIcon?.setImageResource(R.drawable.serious_warning)
    }

    companion object {
        const val ITEM_VIEW_TYPE_LIST: Int = 0
        const val ITEM_VIEW_TYPE_GRID: Int = 1
        const val ITEM_VIEW_TYPE_HEADER: Int = 2
    }
}
