package mega.privacy.android.app.fragments.recent

import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BucketSaved
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentRecentBucketBinding
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.offline.OfflineAdapter
import mega.privacy.android.app.fragments.offline.OfflineListViewHolder
import mega.privacy.android.app.imageviewer.ImageViewerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.PdfViewerActivity
import mega.privacy.android.app.main.adapters.MultipleBucketAdapter
import mega.privacy.android.app.main.controllers.NodeController
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST
import mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER
import mega.privacy.android.app.utils.Constants.RECENTS_BUCKET_ADAPTER
import mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE
import mega.privacy.android.app.utils.Constants.VIEWER_FROM_RECETS_BUCKET
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.MegaNodeUtil.isValidForImageViewer
import mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent
import mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode
import mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.app.utils.Util.mutateIconSecondary
import mega.privacy.android.app.utils.callManager
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment class for the Recents Bucket
 */
@AndroidEntryPoint
class RecentsBucketFragment : Fragment(), ActionMode.Callback {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    private val viewModel by viewModels<RecentsBucketViewModel>()

    private val selectedBucketModel: SelectedBucketViewModel by activityViewModels()

    private lateinit var binding: FragmentRecentBucketBinding

    private lateinit var listView: RecyclerView

    private var adapter: MultipleBucketAdapter? = null

    private var actionMode: ActionMode? = null

    private lateinit var bucket: BucketSaved

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRecentBucketBinding.inflate(inflater, container, false)
        listView = binding.multipleBucketView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        val selectedBucket = selectedBucketModel.selected.value
        bucket = BucketSaved(selectedBucket)
        viewModel.bucket.value = selectedBucket

        viewModel.cachedActionList.value = selectedBucketModel.currentActionList.value

        viewModel.shouldCloseFragment.observe(viewLifecycleOwner) {
            if (it) Navigation.findNavController(view).popBackStack()
        }

        viewModel.items.observe(viewLifecycleOwner) {
            setupListView(it)
            setupHeaderView()
            setupFastScroller(it)
            setupToolbar()
            checkScroll()
        }

        viewModel.actionMode.observe(viewLifecycleOwner) { visible ->
            val actionModeVal = actionMode

            if (visible) {
                if (actionModeVal == null) {
                    callManager {
                        actionMode = it.startSupportActionMode(this)
                        it.setTextSubmitted()
                    }
                }

                actionMode?.title = viewModel.getSelectedNodesCount().toString()
                actionMode?.invalidate()
            } else {
                if (actionModeVal != null) {
                    actionModeVal.finish()
                    actionMode = null
                }
            }
        }

//        observeAnimatedItems()

        observeDragSupportEvents(viewLifecycleOwner, listView, VIEWER_FROM_RECETS_BUCKET)
    }

    private fun setupListView(nodes: List<MegaNode>) {
        if (adapter == null) {
            adapter = MultipleBucketAdapter(activity, this, nodes, bucket.isMedia)
            listView.adapter = adapter

            if (bucket.isMedia) {
                val numCells: Int = if (Util.isScreenInPortrait(activity)) 4 else 6
                val gridLayoutManager =
                    GridLayoutManager(activity, numCells, GridLayoutManager.VERTICAL, false)

                listView.layoutManager = gridLayoutManager
            } else {
                val linearLayoutManager = LinearLayoutManager(activity)

                listView.layoutManager = linearLayoutManager
                listView.addItemDecoration(SimpleDividerItemDecoration(activity))
            }

            listView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    checkScroll()
                }
            })

            listView.clipToPadding = false
            listView.setHasFixedSize(true)
        } else {
            adapter?.setNodes(nodes)
        }
    }

    private fun setupFastScroller(nodes: List<MegaNode>) {
        if (nodes.size >= MIN_ITEMS_SCROLLBAR) {
            binding.fastscroll.visibility = View.VISIBLE
            binding.fastscroll.setRecyclerView(listView)
        } else {
            binding.fastscroll.visibility = View.GONE
        }
    }

    private fun setupHeaderView() {
        if (!bucket.isMedia) {
            val folder = megaApi.getNodeByHandle(bucket.parentHandle) ?: return
            binding.folderNameText.text = folder.name

            binding.actionImage.setImageDrawable(
                mutateIconSecondary(
                    context,
                    if (bucket.isUpdate) R.drawable.ic_versions_small else R.drawable.ic_recents_up,
                    R.color.grey_054_white_054
                )
            )

            binding.dateText.text =
                TimeUtils.formatBucketDate(activity, bucket.timestamp)
            binding.headerInfoLayout.visibility = View.VISIBLE
        }
    }

    private fun setupToolbar() {
        (activity as ManagerActivity).setToolbarTitle(
            "${viewModel.items.value?.size} ${getString(R.string.general_files)}"
        )
    }

    private fun checkScroll() {
        val canScroll = listView.canScrollVertically(-1)
        (activity as ManagerActivity).changeAppBarElevation(canScroll)
    }

    private fun getNodesHandles(isImageViewerValid: Boolean): LongArray? =
        viewModel.items.value?.filter {
            if (isImageViewerValid) {
                it.isValidForImageViewer()
            } else {
                FileUtil.isAudioOrVideo(it) && FileUtil.isInternalIntent(it)
            }
        }?.map { it.handle }?.toLongArray()

    fun openFile(
        index: Int,
        node: MegaNode,
        isMedia: Boolean,
    ) {
        val mime = MimeTypeList.typeForName(node.name)
        val localPath =
            FileUtil.getLocalFile(node)
        Timber.d("Open node: ${node.name} which mime is: ${mime.type}, local path is: $localPath")

        when {
            mime.isImage -> {
                openImage(index, node)
            }
            FileUtil.isAudioOrVideo(node) -> {
                openAudioVideo(index, node, isMedia, localPath)
            }
            mime.isURL -> {
                manageURLNode(requireContext(), megaApi, node)
            }
            mime.isPdf -> {
                openPdf(index, node, localPath)
            }
            mime.isOpenableTextFile(node.size) -> {
                manageTextFileIntent(requireContext(), node, RECENTS_ADAPTER)
            }
            else -> {
                onNodeTapped(
                    requireActivity(),
                    node,
                    {
                        (requireActivity() as ManagerActivity).saveNodeByTap(it)
                    },
                    (requireActivity() as ManagerActivity),
                    (requireActivity() as ManagerActivity)
                )
            }
        }
    }

    fun onNodeLongClicked(position: Int, node: MegaNode) {
        viewModel.onNodeLongClicked(position, node)
    }

    private fun openPdf(
        index: Int,
        node: MegaNode,
        localPath: String?,
    ) {
        val intent = Intent(context, PdfViewerActivity::class.java)
        intent.putExtra(INTENT_EXTRA_KEY_INSIDE, true)
        intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_BUCKET_ADAPTER)
        putThumbnailLocation(intent, listView, index, VIEWER_FROM_RECETS_BUCKET, adapter!!)

        val paramsSetSuccessfully =
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(activity, node, intent, localPath, false,
                    requireActivity() as ManagerActivity
                )
            } else {
                FileUtil.setStreamingIntentParams(activity, node, megaApi, intent,
                    requireActivity() as ManagerActivity
                )
            }
        intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.handle)
        openOrDownload(intent, paramsSetSuccessfully, node.handle)
    }

    private fun openAudioVideo(
        index: Int,
        node: MegaNode,
        isMedia: Boolean,
        localPath: String?,
    ) {
        val intent = if (FileUtil.isInternalIntent(node)) {
            getMediaIntent(activity, node.name)
        } else {
            Intent(Intent.ACTION_VIEW)
        }

        intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_BUCKET_ADAPTER)
        putThumbnailLocation(intent, listView, index, VIEWER_FROM_RECETS_BUCKET, adapter!!)
        intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.name)

        if (isMedia) {
            intent.putExtra(NODE_HANDLES, getNodesHandles(false))
            intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false)
        }

        val paramsSetSuccessfully =
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(activity, node, intent, localPath, false,
                    requireActivity() as ManagerActivity
                )
            } else {
                FileUtil.setStreamingIntentParams(activity, node, megaApi, intent,
                    requireActivity() as ManagerActivity
                )
            }

        if (paramsSetSuccessfully) {
            intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.handle)

            if (FileUtil.isOpusFile(node)) {
                intent.setDataAndType(intent.data, "audio/*")
            }
        }

        openOrDownload(intent, paramsSetSuccessfully, node.handle)
    }

    private fun openOrDownload(
        intent: Intent,
        paramsSetSuccessfully: Boolean,
        handle: Long,
    ) {
        if (paramsSetSuccessfully && MegaApiUtils.isIntentAvailable(activity, intent)) {
            activity?.startActivity(intent)
            activity?.overridePendingTransition(0, 0)
        } else {
            (activity as ManagerActivity).showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.intent_not_available),
                MEGACHAT_INVALID_HANDLE
            )
            download(handle)
        }
    }

    private fun openImage(
        index: Int,
        node: MegaNode,
    ) {
        val handles = getNodesHandles(true)
        val intent = if (handles != null && handles.isNotEmpty()) {
            ImageViewerActivity.getIntentForChildren(
                requireContext(),
                handles,
                node.handle
            )
        } else {
            ImageViewerActivity.getIntentForSingleNode(
                requireContext(),
                node.handle
            )
        }
        putThumbnailLocation(intent, listView, index, VIEWER_FROM_RECETS_BUCKET, adapter!!)
        startActivity(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private fun download(handle: Long) {
        callManager { it.saveHandlesToDevice(listOf(handle), true, false, false, false) }
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onCreateActionMode")
        val inflater = mode!!.menuInflater

        inflater.inflate(R.menu.recents_bucket_action, menu)
        checkScroll()

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        Timber.d("ActionBarCallBack::onPrepareActionMode")

        menu!!.findItem(R.id.cab_menu_select_all).isVisible =
            (viewModel.getSelectedNodesCount() < viewModel.getNodesCount())

        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        Timber.d("ActionBarCallBack::onActionItemClicked")
        val selectedNodes = viewModel.getSelectedNodes()
        val nodesHandles = ArrayList(selectedNodes.map { it.handle })
        when (item!!.itemId) {
            R.id.cab_menu_download -> {
                callManager {
                    it.saveNodesToDevice(selectedNodes,
                        false,
                        false,
                        false,
                        false)
                }
                viewModel.clearSelection()
            }
            R.id.cab_menu_share_link -> {
                callManager {
                    LinksUtil.showGetLinkActivity(
                        it,
                        nodesHandles.toLongArray()
                    )
                }
            }
            R.id.cab_menu_send_to_chat -> {
                callManager {
                    it.attachNodesToChats(selectedNodes)
                }
            }

            R.id.cab_menu_share_out -> {
                callManager {
                    MegaNodeUtil.shareNodes(it, selectedNodes)
                }
                viewModel.clearSelection()
            }
            R.id.cab_menu_select_all -> {
                viewModel.selectAll()
            }
            R.id.cab_menu_clear_selection -> {
                viewModel.clearSelection()
            }
            R.id.cab_menu_move -> {
                callManager {
                    NodeController(it).chooseLocationToMoveNodes(nodesHandles)
                }
            }
            R.id.cab_menu_copy -> {
                callManager {
                    NodeController(it).chooseLocationToCopyNodes(nodesHandles)
                }
            }
            R.id.cab_menu_trash -> {
                callManager {
                    it.askConfirmationMoveToRubbish(nodesHandles)
                }
            }
        }

        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        Timber.d("ActionBarCallBack::onDestroyActionMode")

        viewModel.clearSelection()
        checkScroll()
    }

//    private fun observeAnimatedItems() {
//        var animatorSet: AnimatorSet? = null
//
//        viewModel.nodesToAnimate.observe(viewLifecycleOwner) {
//            val rvAdapter = adapter ?: return@observe
//
//            animatorSet?.run {
//                // End the started animation if any, or the view may show messy as its property
//                // would be wrongly changed by multiple animations running at the same time
//                // via contiguous quick clicks on the item
//                if (isStarted) {
//                    end()
//                }
//            }
//
//            // Must create a new AnimatorSet, or it would keep all previous
//            // animation and play them together
//            animatorSet = AnimatorSet()
//            val animatorList = mutableListOf<Animator>()
//
//            animatorSet?.addListener(object : Animator.AnimatorListener {
//                override fun onAnimationRepeat(animation: Animator) {
//                }
//
//                override fun onAnimationEnd(animation: Animator) {
//                    viewModel.items.value?.let { newList ->
//                        rvAdapter.(ArrayList(newList))
//                    }
//                }
//
//                override fun onAnimationCancel(animation: Animator) {
//                }
//
//                override fun onAnimationStart(animation: Animator) {
//                }
//            })
//
//            it.forEach { pos ->
//                recyclerView?.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
//                    val itemView = viewHolder.itemView
//
//                    val imageView: ImageView? = when (rvAdapter.getItemViewType(pos)) {
//                        OfflineAdapter.TYPE_LIST -> {
//                            val thumbnail = itemView.findViewById<ImageView>(R.id.thumbnail)
//                            val param = thumbnail.layoutParams as FrameLayout.LayoutParams
//                            param.width = Util.dp2px(
//                                OfflineListViewHolder.LARGE_IMAGE_WIDTH,
//                                resources.displayMetrics
//                            )
//                            param.height = param.width
//                            param.marginStart = Util.dp2px(
//                                OfflineListViewHolder.LARGE_IMAGE_MARGIN_LEFT,
//                                resources.displayMetrics
//                            )
//                            thumbnail.layoutParams = param
//                            thumbnail
//                        }
//                        OfflineAdapter.TYPE_GRID_FOLDER -> {
//                            itemView.background = ContextCompat.getDrawable(
//                                requireContext(), R.drawable.background_item_grid_selected
//                            )
//                            itemView.findViewById(R.id.icon)
//                        }
//                        OfflineAdapter.TYPE_GRID_FILE -> {
//                            itemView.background = ContextCompat.getDrawable(
//                                requireContext(), R.drawable.background_item_grid_selected
//                            )
//                            itemView.findViewById(R.id.ic_selected)
//                        }
//                        else -> null
//                    }
//
//                    imageView?.run {
//                        setImageResource(R.drawable.ic_select_folder)
//                        visibility = View.VISIBLE
//
//                        val animator =
//                            AnimatorInflater.loadAnimator(context, R.animator.icon_select)
//                        animator.setTarget(this)
//                        animatorList.add(animator)
//                    }
//                }
//            }
//
//            animatorSet?.playTogether(animatorList)
//            animatorSet?.start()
//        }
//    }

    fun handleItemClick(position: Int, node: MegaNode, isMedia: Boolean) {
        if (actionMode == null) {
            openFile(position, node, isMedia)
        } else {
            viewModel.onNodeLongClicked(position, node)
        }

    }
}
