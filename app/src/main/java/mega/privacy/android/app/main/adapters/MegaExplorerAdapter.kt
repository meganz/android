package mega.privacy.android.app.main.adapters

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.util.forEach
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.MimeTypeThumbnail
import mega.privacy.android.app.R
import mega.privacy.android.app.components.NewGridRecyclerView
import mega.privacy.android.app.components.scrollBar.SectionTitleProvider
import mega.privacy.android.app.databinding.ItemFileExplorerBinding
import mega.privacy.android.app.databinding.ItemFileExplorerGridBinding
import mega.privacy.android.app.databinding.SortByHeaderBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.main.CloudDriveExplorerFragment
import mega.privacy.android.app.main.DrawerItem
import mega.privacy.android.app.main.IncomingSharesExplorerFragment
import mega.privacy.android.app.main.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
import mega.privacy.android.app.main.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_HEADER
import mega.privacy.android.app.main.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
import mega.privacy.android.app.utils.ColorUtils.getThemeColor
import mega.privacy.android.app.utils.Constants.ICON_MARGIN_DP
import mega.privacy.android.app.utils.Constants.ICON_SIZE_DP
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.Constants.THUMB_CORNER_RADIUS_DP
import mega.privacy.android.app.utils.Constants.THUMB_MARGIN_DP
import mega.privacy.android.app.utils.Constants.THUMB_SIZE_DP
import mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB
import mega.privacy.android.app.utils.FileUtil.isVideoFile
import mega.privacy.android.app.utils.MegaApiUtils.getMegaNodeFolderInfo
import mega.privacy.android.app.utils.MegaNodeUtil.getFileInfo
import mega.privacy.android.app.utils.MegaNodeUtil.getFolderIcon
import mega.privacy.android.app.utils.MegaNodeUtil.getNumberOfFolders
import mega.privacy.android.app.utils.ThumbnailUtils
import mega.privacy.android.app.utils.TimeUtils.getVideoDuration
import mega.privacy.android.app.utils.Util.dp2px
import mega.privacy.android.app.utils.Util.scaleWidthPx
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

/**
 * The adapter for Mega Explorer page
 */
class MegaExplorerAdapter(
    private val context: Context,
    private val fragment: Fragment,
    nodes: List<MegaNode?>,
    parentHandle: Long,
    private val recyclerView: RecyclerView,
    private val selectFile: Boolean,
    private val sortByViewModel: SortByHeaderViewModel,
    private val megaApi: MegaApiAndroid,
) : RecyclerView.Adapter<MegaExplorerAdapter.ViewHolderExplorer>(), SectionTitleProvider,
    RotatableAdapter {

    private val data = mutableListOf<MegaNode?>()

    /**
     * Parent handle
     */
    var parentHandle: Long = INVALID_HANDLE

    private val selectedItems: SparseBooleanArray = SparseBooleanArray()

    /**
     * Whether multiple selected
     */
    var multipleSelected: Boolean = false
        set(value) {
            Timber.d("multipleSelect: $value")
            if (field != value) {
                field = value
            }
        }

    private var placeholderCount: Int = 0

    private var outMetrics: DisplayMetrics

    init {
        this.parentHandle = parentHandle
        data.addAll(nodes)
        outMetrics = context.resources.displayMetrics
    }

    override fun getItemCount(): Int = data.size

    override fun getItemViewType(position: Int): Int =
        if (data.isNotEmpty() && position == 0) {
            ITEM_VIEW_TYPE_HEADER
        } else {
            if (sortByViewModel.isListView()) {
                ITEM_VIEW_TYPE_LIST
            } else {
                ITEM_VIEW_TYPE_GRID
            }
        }

    override fun getItemId(position: Int): Long = position.toLong()

    /**
     * Get item by position
     *
     * @param position the item position
     * @return MegaNode item
     */
    fun getItem(position: Int): MegaNode? =
        if (data.isNotEmpty() && position >= 0 && position < data.size) data[position]
        else null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolderExplorer = when (viewType) {
        ITEM_VIEW_TYPE_LIST -> {
            Timber.d("onCreateViewHolder list")
            ItemFileExplorerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .let { binding ->
                    ViewHolderListExplorer(binding).let { holder ->
                        binding.root.tag = holder
                        holder
                    }
                }
        }
        ITEM_VIEW_TYPE_GRID -> {
            Timber.d("onCreateViewHolder grid")
            ItemFileExplorerGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .let { binding ->
                    ViewHolderGridExplorer(binding).let { holder ->
                        binding.root.tag = holder
                        holder
                    }
                }
        }
        else -> {
            ViewHolderSortBy(SortByHeaderBinding.inflate(LayoutInflater.from(parent.context),
                parent,
                false))
        }
    }

    override fun onBindViewHolder(holder: ViewHolderExplorer, position: Int) {
        when (getItemViewType(position)) {
            ITEM_VIEW_TYPE_HEADER -> (holder as ViewHolderSortBy).bind()
            ITEM_VIEW_TYPE_LIST -> getItem(position)?.let { node ->
                (holder as ViewHolderListExplorer).bind(position, node)
            }
            ITEM_VIEW_TYPE_GRID -> (holder as ViewHolderGridExplorer).bind(position,
                getItem(position))
        }
    }

    private fun setImageParams(image: ImageView, size: Int, marginSize: Int) {
        (image.layoutParams as RelativeLayout.LayoutParams).let { params ->
            params.width = dp2px(size.toFloat())
            params.height = dp2px(size.toFloat())

            params.setMargins(dp2px(marginSize.toFloat()))
            image.layoutParams = params
        }
    }

    /**
     * Toggle selection
     *
     * @param position the item position
     */
    fun toggleSelection(position: Int) {
        Timber.d("toggleSelection: $position")
        startAnimation(position, putOrDeletePosition(position))
    }

    private fun putOrDeletePosition(position: Int) =
        if (selectedItems.get(position, false)) {
            Timber.d("delete pos: $position")
            selectedItems.delete(position)
            true
        } else {
            Timber.d("PUT pos: $position")
            selectedItems.put(position, true)
            false
        }

    private fun hideMultipleSelect() {
        if (selectedItems.size() <= 0) {
            when (fragment) {
                is CloudDriveExplorerFragment -> fragment.hideMultipleSelect()
                is IncomingSharesExplorerFragment -> fragment.hideMultipleSelect()
            }
        }
    }

    private fun startAnimation(position: Int, delete: Boolean) {
        if (sortByViewModel.isListView()) {
            Timber.d("adapter type is LIST")
            recyclerView.findViewHolderForAdapterPosition(position)?.let { view ->
                Timber.d("Start animation: $position")
                AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
                    .let { flipAnimation ->
                        flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(p0: Animation?) {
                                if (!delete) {
                                    notifyItemChanged(position)
                                }
                            }

                            override fun onAnimationEnd(p0: Animation?) {
                                hideMultipleSelect()
                                if (delete) {
                                    notifyItemChanged(position)
                                }
                            }

                            override fun onAnimationRepeat(p0: Animation?) {
                            }

                        })
                        (view as ViewHolderListExplorer).imageView.startAnimation(flipAnimation)
                    }
            } ?: let {
                Timber.d("view is null - not animation")
                hideMultipleSelect()
                notifyItemChanged(position)
            }
        } else {
            Timber.d("adapter type is GRID")
            recyclerView.findViewHolderForAdapterPosition(position)?.let { view ->
                Timber.d("Start animation: $position")
                AnimationUtils.loadAnimation(context, R.anim.multiselect_flip)
                    .let { flipAnimation ->
                        if (!delete) {
                            notifyItemChanged(position)
                            flipAnimation.duration = ANIMATION_DURATION
                        }

                        flipAnimation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(p0: Animation?) {
                                if (!delete) {
                                    notifyItemChanged(position)
                                }
                            }

                            override fun onAnimationEnd(p0: Animation?) {
                                hideMultipleSelect()
                                notifyItemChanged(position)
                            }

                            override fun onAnimationRepeat(p0: Animation?) {
                            }
                        })

                        (view as ViewHolderGridExplorer).fileSelectedIcon.startAnimation(
                            flipAnimation)
                    }
            } ?: let {
                Timber.d("view is null - not animation")
                hideMultipleSelect()
                notifyItemChanged(position)
            }
        }
    }

    /**
     * Select all items
     */
    fun selectAll() {
        data.mapIndexed { index, node ->
            if (node != null && !node.isFolder && !node.isTakenDown && !isItemChecked(index)) {
                toggleSelection(index)
            }
        }
    }

    /**
     * Clear all selections
     */
    fun clearSelections() {
        Timber.d("clearSelections")
        for (i in 0 until itemCount) {
            if (isItemChecked(i)) {
                toggleSelection(i)
            }
        }
    }

    private fun isItemChecked(position: Int) = selectedItems.get(position)

    /**
     * Get selected items count
     */
    fun getSelectedItemCount() = selectedItems.size()

    override fun getSelectedItems(): MutableList<Int> {
        val result = mutableListOf<Int>()
        selectedItems.forEach { key, _ ->
            result.add(key)
        }
        return result
    }

    override fun getSectionTitle(position: Int): String? =
        getItem(position)?.let { node ->
            if (node.name != null && node.name.isNotEmpty()) {
                node.name.substring(0, 1)
            } else {
                null
            }
        }

    override fun getFolderCount(): Int = getNumberOfFolders(data)

    override fun getPlaceholderCount(): Int = placeholderCount

    override fun getUnhandledItem(): Int = -1

    private fun clickItem(view: View) {
        (view.tag as ViewHolderExplorer).let { holder ->
            when (fragment) {
                is CloudDriveExplorerFragment -> fragment.itemClick(holder.absoluteAdapterPosition)
                is IncomingSharesExplorerFragment -> fragment.itemClick(view,
                    holder.absoluteAdapterPosition)
            }
        }
    }

    /**
     * Get selected nodes
     *
     * @return selected items
     */
    fun getSelectedNodes(): List<MegaNode> {
        val result = mutableListOf<MegaNode>()
        selectedItems.forEach { key, value ->
            if (value) {
                getNodeAt(key)?.let { node ->
                    result.add(node)
                }
            }
        }
        return result
    }

    /**
     * Get the handles of selected items
     *
     * @return handles of selected items
     */
    fun getSelectedHandles(): LongArray {
        val handles = mutableListOf<Long>()
        selectedItems.forEach { key, value ->
            if (value) {
                getNodeAt(key)?.let { node ->
                    handles.add(node.handle)
                }
            }
        }
        return handles.toLongArray()
    }

    private fun getNodeAt(position: Int): MegaNode? =
        if (data.isNotEmpty() && position >= 0 && position < data.size) {
            data[position]
        } else {
            null
        }

    /**
     * Set nodes
     *
     * @param nodes MegaNode items
     */
    fun setNodes(nodes: List<MegaNode?>) {
        data.clear()
        data.addAll(insertPlaceHolderNode(nodes))
        notifyDataSetChanged()
        visibilityFastScroller()
    }

    /**
     * Get nodes
     */
    fun getNodes() = data

    /**
     * Get SpanSizeLookup of [GridLayoutManager]
     *
     * @return [GridLayoutManager.SpanSizeLookup]
     */
    fun getSpanSizeLookup(spanCount: Int): GridLayoutManager.SpanSizeLookup =
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (getItemViewType(position) == ITEM_VIEW_TYPE_HEADER) {
                    spanCount
                } else {
                    1
                }
            }
        }

    /**
     * Insert placeholder node
     *
     * @param nodes the original nodes
     * @return The nodes after added the placeholders
     */
    private fun insertPlaceHolderNode(nodes: List<MegaNode?>): List<MegaNode?> {
        val result = nodes.toMutableList()
        if (sortByViewModel.isListView()) {
            if (result.isNotEmpty()) {
                placeholderCount = 1
                result.add(0, null)
            } else {
                placeholderCount = 0
            }
        } else {
            val folderCount = getNumberOfFolders(nodes)
            var spanCount = 2

            if (recyclerView is NewGridRecyclerView) {
                spanCount = recyclerView.spanCount
            }

            placeholderCount = if ((folderCount % spanCount) == 0) {
                0
            } else {
                spanCount - (folderCount % spanCount)
            }

            if (folderCount > 0 && placeholderCount != 0 && !sortByViewModel.isListView()) {
                //Add placeholder at folders' end.
                for (i in 0 until placeholderCount) {
                    result.add(folderCount + i, null)
                }
            }

            if (result.isNotEmpty()) {
                placeholderCount++
                result.add(0, null)
            }
        }
        return result
    }

    /**
     * Set visibility for FastScroller
     */
    private fun visibilityFastScroller() {
        val visible = itemCount >= MIN_ITEMS_SCROLLBAR

        when (fragment) {
            is IncomingSharesExplorerFragment -> fragment.fastScroller.isVisible = visible
            is CloudDriveExplorerFragment -> fragment.getFastScroller().isVisible = visible
        }
    }

    /**
     * The open class for explorer view holder
     */
    open class ViewHolderExplorer(itemView: View) : RecyclerView.ViewHolder(itemView) {

        /**
         * Current item position
         */
        var currentPosition: Int = 0

        /**
         * Document
         */
        @JvmField
        var document: Long = 0
    }

    /**
     * The inner class for list explorer view holder
     */
    inner class ViewHolderListExplorer(private val binding: ItemFileExplorerBinding) :
        ViewHolderExplorer(binding.root) {

        /**
         * The thumbnail icon
         */
        lateinit var imageView: ImageView

        /**
         * Bind view
         */
        fun bind(position: Int, node: MegaNode) {
            with(binding) {
                imageView = binding.fileExplorerThumbnail
                itemView.setOnClickListener(null)
                itemView.setOnLongClickListener(null)

                currentPosition = position
                document = node.handle
                binding.fileExplorerFilename.text = node.name
                imageView.alpha = 1.0f

                binding.fileExplorerFilename.setTextColor(
                    getThemeColor(context,
                        if (node.isTakenDown)
                            R.attr.colorError
                        else
                            android.R.attr.textColorPrimary))
                binding.fileListTakenDown.isVisible = node.isTakenDown

                if (node.isFolder) {
                    setImageParams(imageView, ICON_SIZE_DP, ICON_MARGIN_DP)
                    itemView.setOnClickListener(::clickItem)
                    binding.fileExplorerPermissions.isVisible = false
                    binding.fileExplorerFilesize.text = getMegaNodeFolderInfo(node)
                    imageView.setImageResource(getFolderIcon(node,
                        DrawerItem.CLOUD_DRIVE))

                    if (node.isInShare) {
                        if (context.resources.configuration.orientation == ORIENTATION_LANDSCAPE) {
                            scaleWidthPx(MAX_WIDTH_FILENAME_LAND, outMetrics)
                        } else {
                            scaleWidthPx(MAX_WIDTH_FILENAME_PORT, outMetrics)
                        }.let { width ->
                            binding.fileExplorerFilename.maxWidth = width
                            binding.fileExplorerFilesize.maxWidth = width
                        }

                        megaApi.inSharesList.map { megaShare ->
                            if (megaShare.nodeHandle == node.handle) {
                                binding.fileExplorerFilesize.text =
                                    megaApi.getContact(megaShare.user)?.let { user ->
                                        getMegaUserNameDB(user)
                                    } ?: megaShare.user
                            }
                        }

                        // Check permissions
                        binding.fileExplorerPermissions.isVisible = true
                        megaApi.getAccess(node).let { accessLevel ->
                            binding.fileExplorerPermissions.setImageResource(when (accessLevel) {
                                MegaShare.ACCESS_FULL -> R.drawable.ic_shared_fullaccess
                                MegaShare.ACCESS_READ -> R.drawable.ic_shared_read
                                else -> R.drawable.ic_shared_read_write
                            })
                        }
                        return@with
                    }
                } else {
                    binding.fileExplorerPermissions.isVisible = false

                    binding.fileExplorerFilesize.text = getFileInfo(node)
                    imageView.setImageResource(
                        MimeTypeList.typeForName(node.name).iconResourceId)
                    setImageParams(imageView, ICON_SIZE_DP, ICON_MARGIN_DP)

                    if (selectFile) {
                        if (!node.isTakenDown) {
                            itemView.setOnClickListener(::clickItem)
                            itemView.setOnLongClickListener {
                                clickItem(it)
                                true
                            }
                        }

                        if (multipleSelected && isItemChecked(position)) {
                            imageView.setImageResource(R.drawable.ic_select_folder)
                            Timber.d("Do not show thumb")
                            return
                        } else {
                            imageView.setImageResource(MimeTypeList.typeForName(
                                node.name).iconResourceId)
                            binding.fileExplorerItemLayout.background = null
                        }
                    } else {
                        imageView.alpha = 0.4f
                        binding.fileExplorerFilename.setTextColor(getThemeColor(context,
                            android.R.attr.textColorSecondary))
                    }

                    var thumbnail = ThumbnailUtils.getThumbnailFromCache(node)
                        ?: ThumbnailUtils.getThumbnailFromFolder(node, context)
                    if (thumbnail == null) {
                        runCatching {
                            if (node.hasThumbnail()) {
                                thumbnail = ThumbnailUtils.getThumbnailFromMegaExplorer(node,
                                    context,
                                    this@ViewHolderListExplorer,
                                    megaApi,
                                    this@MegaExplorerAdapter)
                            } else {
                                ThumbnailUtils.createThumbnailExplorer(context,
                                    node,
                                    this@ViewHolderListExplorer,
                                    megaApi,
                                    this@MegaExplorerAdapter)
                            }
                        }.onFailure {
                            Timber.e(it)
                        }
                    }

                    thumbnail?.let {
                        setImageParams(imageView, THUMB_SIZE_DP, THUMB_MARGIN_DP)
                        imageView.setImageBitmap(ThumbnailUtils.getRoundedBitmap(
                            context,
                            it,
                            dp2px(THUMB_CORNER_RADIUS_DP)))
                    }
                }
            }
        }
    }

    /**
     * The inner class for grid explorer view holder
     */
    inner class ViewHolderGridExplorer(private val binding: ItemFileExplorerGridBinding) :
        ViewHolderExplorer(binding.root) {

        /**
         * File selected icon
         */
        val fileSelectedIcon: ImageView = binding.fileExplorerGridFileSelected

        /**
         * File thumbnail icon
         */
        lateinit var fileThumbnail: ImageView

        /**
         * File icon
         */
        lateinit var fileIcon: ImageView

        /**
         * Bind view
         */
        fun bind(position: Int, node: MegaNode?) {
            Timber.d("onBindViewHolderGrid")
            currentPosition = position
            fileThumbnail = binding.fileExplorerGridFileThumbnail
            fileIcon = binding.fileExplorerGridFileIcon

            if (node == null) {
                binding.fileExplorerGridFileLayout.isVisible = false
                binding.fileExplorerGridFolderLayout.visibility = View.INVISIBLE
                binding.fileExplorerGridLayout.visibility = View.INVISIBLE
                return
            }

            itemView.setOnClickListener(null)
            itemView.setOnLongClickListener(null)

            document = node.handle
            binding.fileExplorerGridLayout.isVisible = true

            if (node.isFolder) {
                binding.fileExplorerGridFolderLayout.isVisible = true
                binding.fileExplorerGridFileLayout.isVisible = false
                binding.fileExplorerGridFolderFilename.text = node.name
                binding.fileExplorerGridFolderIcon.setImageResource(getFolderIcon(
                    node,
                    DrawerItem.CLOUD_DRIVE))
                itemView.setOnClickListener(::clickItem)

                binding.fileExplorerGridFolderFilename.setTextColor(
                    getThemeColor(context,
                        if (node.isTakenDown)
                            R.attr.colorError
                        else
                            android.R.attr.textColorPrimary))
                binding.fileGridTakenDown.isVisible = node.isTakenDown
            } else {
                binding.fileExplorerGridFolderLayout.isVisible = false
                binding.fileExplorerGridFileLayout.isVisible = true
                binding.fileGridFilenameForFile.text = node.name
                fileThumbnail.isVisible = false
                fileIcon.setImageResource(MimeTypeThumbnail.typeForName(
                    node.name).iconResourceId)

                binding.fileGridFilenameForFile.setTextColor(
                    getThemeColor(context,
                        if (node.isTakenDown)
                            R.attr.colorError
                        else
                            android.R.attr.textColorPrimary))
                binding.fileGridTakenDownForFile.isVisible = node.isTakenDown

                if (isVideoFile(node.name)) {
                    binding.fileExplorerGridFileVideoinfoLayout.isVisible = true
                    Timber.d("${node.name} DURATION: ${node.duration}")

                    val duration = getVideoDuration(node.duration)
                    if (duration.isEmpty()) {
                        binding.fileExplorerGridFileTitleVideoDuration.isVisible = false
                    } else {
                        binding.fileExplorerGridFileTitleVideoDuration.text = duration
                        binding.fileExplorerGridFileTitleVideoDuration.isVisible = true
                    }
                } else {
                    binding.fileExplorerGridFileVideoinfoLayout.isVisible = false
                }

                var thumbnail =
                    ThumbnailUtils.getThumbnailFromCache(node)
                        ?: ThumbnailUtils.getThumbnailFromFolder(node,
                            context)
                if (thumbnail == null) {
                    runCatching {
                        if (node.hasThumbnail()) {
                            thumbnail =
                                ThumbnailUtils.getThumbnailFromMegaExplorer(
                                    node,
                                    context,
                                    this@ViewHolderGridExplorer,
                                    megaApi,
                                    this@MegaExplorerAdapter)
                        } else {
                            ThumbnailUtils.createThumbnailExplorer(
                                context,
                                node,
                                this@ViewHolderGridExplorer,
                                megaApi,
                                this@MegaExplorerAdapter)
                        }
                    }.onFailure {
                        Timber.e(it)
                    }
                }

                thumbnail?.let {
                    fileThumbnail.setImageBitmap(
                        ThumbnailUtils.getRoundedRectBitmap(
                            context,
                            thumbnail,
                            2))
                    fileThumbnail.isVisible = true
                    fileIcon.isVisible = false
                } ?: let {
                    fileThumbnail.isVisible = false
                    fileIcon.isVisible = true
                }

                if (selectFile) {
                    fileThumbnail.alpha = 1.0f

                    if (!node.isTakenDown) {
                        itemView.setOnClickListener(::clickItem)
                        itemView.setOnLongClickListener {
                            clickItem(it)
                            true
                        }
                    }

                    if (multipleSelected && isItemChecked(position)) {
                        binding.fileExplorerGridLayout.background =
                            ContextCompat.getDrawable(context,
                                R.drawable.background_item_grid_selected)
                        fileSelectedIcon.setImageResource(R.drawable.ic_select_folder)
                    } else {
                        binding.fileExplorerGridLayout.background =
                            ContextCompat.getDrawable(context,
                                R.drawable.background_item_grid)
                        fileSelectedIcon.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
                    }
                } else {
                    fileThumbnail.alpha = 0.4f
                }
            }
        }
    }

    /**
     * The inner class for sort by view holder
     */
    inner class ViewHolderSortBy(private val binding: SortByHeaderBinding) :
        ViewHolderExplorer(binding.root) {

        internal fun bind() {
            binding.sortByHeaderViewModel = sortByViewModel
            SortByHeaderViewModel.orderNameMap[if (fragment is IncomingSharesExplorerFragment && parentHandle == INVALID_HANDLE) {
                sortByViewModel.order.second
            } else {
                sortByViewModel.order.first
            }]?.let {
                binding.orderNameStringId = it
            }
            binding.enterMediaDiscovery.isVisible = false
        }
    }

    companion object {
        private const val ANIMATION_DURATION: Long = 250
        private const val MAX_WIDTH_FILENAME_LAND = 260
        private const val MAX_WIDTH_FILENAME_PORT = 200
    }
}