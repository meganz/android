package mega.privacy.android.app.presentation.recentactions.recentactionbucket

import mega.privacy.android.core.R as CoreUiR
import android.animation.Animator
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.generic.RoundingParams
import com.facebook.drawee.view.SimpleDraweeView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.observeDragSupportEvents
import mega.privacy.android.app.components.dragger.DragToExitSupport.Companion.putThumbnailLocation
import mega.privacy.android.app.databinding.FragmentRecentActionBucketBinding
import mega.privacy.android.app.fragments.homepage.NodeItem
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewActivity
import mega.privacy.android.app.presentation.imagepreview.fetcher.DefaultImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewMenuSource
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity
import mega.privacy.android.app.presentation.recentactions.RecentActionsComposeViewModel
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
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeUtil.isGif
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.getMediaIntent
import mega.privacy.android.app.utils.Util.mutateIconSecondary
import mega.privacy.android.app.utils.callManager
import mega.privacy.android.app.utils.wrapper.MegaNodeUtilWrapper
import mega.privacy.android.data.qualifier.MegaApi
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment class for the Recents Bucket
 */
@AndroidEntryPoint
class RecentActionBucketFragment : Fragment() {

    @Inject
    @MegaApi
    lateinit var megaApi: MegaApiAndroid

    @Inject
    lateinit var getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase

    @Inject
    lateinit var megaNodeUtilWrapper: MegaNodeUtilWrapper

    private val viewModel by viewModels<RecentActionBucketViewModel>()

    private val recentActionsComposeViewModel: RecentActionsComposeViewModel by activityViewModels()

    private lateinit var binding: FragmentRecentActionBucketBinding

    private lateinit var listView: RecyclerView

    private var adapter: RecentActionBucketAdapter? = null

    private var actionMode: ActionMode? = null

    private val hiddenNodesOnboardingLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleHiddenNodesOnboardingResult,
        )

    private var tempNodeIds: List<NodeId> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentRecentActionBucketBinding.inflate(inflater, container, false)
        listView = binding.multipleBucketView
        listView.layoutManager = LinearLayoutManager(requireContext())
        listView.itemAnimator = null
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner

        viewLifecycleOwner.lifecycleScope.launch {
            if (!viewModel.isCurrentBucketSet()) {
                viewModel.setBucket(recentActionsComposeViewModel.selectedBucket)
                viewModel.setCachedActionList(recentActionsComposeViewModel.getAllRecentBuckets())
            }
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collectLatest {
                    setupListView(it)
                    setupHeaderView()
                    setupFastScroller(it)
                    setupToolbar()
                    checkScroll()
                }
            }
        }

        viewModel.shouldCloseFragment.observe(viewLifecycleOwner) {
            if (it) Navigation.findNavController(view).popBackStack()
        }

        viewModel.actionMode.observe(viewLifecycleOwner) { visible ->
            if (visible && actionMode == null) {
                callManager { activity ->
                    val actionModeCallback =
                        RecentActionBucketActionModeCallback(
                            activity,
                            this@RecentActionBucketFragment,
                            viewModel,
                            viewModel.isInShare
                        )
                    actionMode = activity.startSupportActionMode(actionModeCallback)
                    activity.setTextSubmitted()
                }
            }
            actionMode?.let {
                if (visible) {
                    it.title = viewModel.getSelectedNodesCount().toString()
                    it.invalidate()
                } else {
                    it.finish()
                    actionMode = null
                }
            }
        }

        observeAnimatedItems()

        observeDragSupportEvents(viewLifecycleOwner, listView, VIEWER_FROM_RECETS_BUCKET)
    }

    private fun setupListView(nodes: List<NodeItem>) {
        if (adapter == null) {
            adapter =
                RecentActionBucketAdapter(
                    activity,
                    this,
                    nodes,
                    viewModel.bucket.value?.isMedia ?: false,
                    viewModel.isInShare,
                    RecentActionBucketDiffCallback()
                )
            listView.adapter = adapter

            if (viewModel.bucket.value?.isMedia == true) {
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
            adapter?.setIsIncomingShare(viewModel.isInShare)
            adapter?.setNodes(nodes)
        }
    }

    private fun setupFastScroller(nodes: List<NodeItem>) {
        if (nodes.size >= MIN_ITEMS_SCROLLBAR) {
            binding.fastscroll.visibility = View.VISIBLE
            binding.fastscroll.setRecyclerView(listView)
        } else {
            binding.fastscroll.visibility = View.GONE
        }
    }

    private fun setupHeaderView() {
        if (viewModel.bucket.value?.isMedia == false) {
            val folder =
                megaApi.getNodeByHandle(viewModel.bucket.value?.parentNodeId?.longValue ?: return)
                    ?: return
            binding.folderNameText.text = folder.name

            binding.actionImage.setImageDrawable(
                mutateIconSecondary(
                    context,
                    if (viewModel.bucket.value?.isUpdate == true) R.drawable.ic_versions_small else R.drawable.ic_recents_up,
                    R.color.grey_054_white_054
                )
            )

            binding.dateText.text =
                TimeUtils.formatBucketDate(
                    viewModel.bucket.value?.timestamp ?: return,
                    requireContext()
                )
            binding.headerInfoLayout.visibility = View.VISIBLE
        }
    }

    fun setupToolbar() {
        (activity as ManagerActivity).setToolbarTitle(
            "${viewModel.items.value.size} ${getString(R.string.general_files)}"
        )
    }

    private fun checkScroll() {
        val canScroll = listView.canScrollVertically(-1)
        (activity as ManagerActivity).changeAppBarElevation(canScroll)
    }

    private fun getNodesHandles(isImageViewerValid: Boolean): LongArray =
        viewModel.items.value.filter {
            if (isImageViewerValid) {
                it.node?.isValidForImageViewer() ?: false
            } else {
                FileUtil.isAudioOrVideo(it.node) && FileUtil.isInternalIntent(it.node)
            }
        }.map { it.node?.handle ?: 0L }.toLongArray()

    private fun MegaNode.isValidForImageViewer(): Boolean =
        isFile && (isImage() || isGif() || isVideo())

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
                megaNodeUtilWrapper.manageURLNode(requireContext(), megaApi, node)
            }

            mime.isPdf -> {
                openPdf(index, node, localPath)
            }

            mime.isOpenableTextFile(node.size) -> {
                megaNodeUtilWrapper.manageTextFileIntent(requireContext(), node, RECENTS_ADAPTER)
            }

            else -> {
                megaNodeUtilWrapper.onNodeTapped(
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

    fun onNodeLongClicked(position: Int, node: NodeItem) {
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
        putThumbnailLocation(intent, listView, index, VIEWER_FROM_RECETS_BUCKET, adapter ?: return)

        val paramsSetSuccessfully =
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(
                    activity, node, intent, localPath, false,
                    requireActivity() as ManagerActivity
                )
            } else {
                FileUtil.setStreamingIntentParams(
                    activity, node, megaApi, intent,
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
        putThumbnailLocation(intent, listView, index, VIEWER_FROM_RECETS_BUCKET, adapter ?: return)
        intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.name)

        if (isMedia) {
            intent.putExtra(NODE_HANDLES, getNodesHandles(false))
            intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false)
        }

        val paramsSetSuccessfully =
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(
                    activity, node, intent, localPath, false,
                    requireActivity() as ManagerActivity
                )
            } else {
                FileUtil.setStreamingIntentParams(
                    activity, node, megaApi, intent,
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
    ) = viewLifecycleOwner.lifecycleScope.launch {
        val handles = getNodesHandles(true)
        val nodeIds = if (handles.isNotEmpty()) handles else longArrayOf(node.handle)
        val menuOptionSource = if (megaApi.getRootParentNode(node).isInShare) {
            ImagePreviewMenuSource.SHARED_ITEMS
        } else {
            ImagePreviewMenuSource.DEFAULT
        }
        val intent = ImagePreviewActivity.createIntent(
            context = requireContext(),
            imageSource = ImagePreviewFetcherSource.DEFAULT,
            menuOptionsSource = menuOptionSource,
            anchorImageNodeId = NodeId(node.handle),
            params = mapOf(DefaultImageNodeFetcher.NODE_IDS to nodeIds),
        )
        putThumbnailLocation(
            intent,
            listView,
            index,
            VIEWER_FROM_RECETS_BUCKET,
            adapter ?: return@launch
        )
        startActivity(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private fun MegaApiJava.getRootParentNode(node: MegaNode): MegaNode {
        var rootParent = node
        while (getParentNode(rootParent) != null) {
            rootParent = getParentNode(rootParent)!!
        }
        return rootParent
    }

    private fun download(handle: Long) {
        callManager { it.saveHandlesToDevice(listOf(handle), true) }
    }

    private fun observeAnimatedItems() {
        var animatorSet: AnimatorSet? = null

        viewModel.nodesToAnimate.observe(viewLifecycleOwner) {
            val rvAdapter = adapter ?: return@observe

            animatorSet?.run {
                // End the started animation if any, or the view may show messy as its property
                // would be wrongly changed by multiple animations running at the same time
                // via contiguous quick clicks on the item
                if (isStarted) {
                    end()
                }
            }

            // Must create a new AnimatorSet, or it would keep all previous
            // animation and play them together
            animatorSet = AnimatorSet()
            val animatorList = mutableListOf<Animator>()

            animatorSet?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    viewModel.items.value.let { newList ->
                        rvAdapter.submitList(ArrayList(newList))
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationStart(animation: Animator) {
                }
            })

            it.forEach { pos ->
                listView.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView

                    val imageView: ImageView = if (viewModel.bucket.value?.isMedia == false) {
                        val thumbnail = itemView.findViewById<ImageView>(R.id.thumbnail_list)
                        val param = thumbnail?.layoutParams as RelativeLayout.LayoutParams
                        param.width = Util.dp2px(
                            48f,
                            resources.displayMetrics
                        )
                        param.height = param.width
                        param.marginStart = Util.dp2px(
                            12f,
                            resources.displayMetrics
                        )
                        thumbnail.layoutParams = param
                        thumbnail
                    } else {
                        val thumbnail =
                            itemView.findViewById<SimpleDraweeView>(R.id.thumbnail_media)

                        thumbnail.hierarchy.roundingParams = RoundingParams.fromCornersRadius(
                            requireContext().resources.getDimensionPixelSize(
                                R.dimen.cu_fragment_selected_round_corner_radius
                            ).toFloat()
                        )
                        thumbnail.background =
                            ContextCompat.getDrawable(
                                requireContext(), R.drawable.background_item_grid_selected
                            )
                        itemView.findViewById(R.id.icon_selected)
                    }

                    imageView.run {
                        setImageResource(CoreUiR.drawable.ic_select_folder)
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(requireContext(), R.animator.icon_select)
                        animator.setTarget(this)
                        animatorList.add(animator)
                    }
                }
            }

            animatorSet?.playTogether(animatorList)
            animatorSet?.start()
        }
    }

    fun handleItemClick(position: Int, node: NodeItem, isMedia: Boolean) {
        if (actionMode == null) {
            openFile(position, node.node ?: return, isMedia)
        } else {
            viewModel.onNodeLongClicked(position, node)
        }
    }

    fun onHideClicked(nodeIds: List<NodeId>) {
        val isHiddenNodesOnboarded = viewModel.state.value.isHiddenNodesOnboarded ?: false
        val isPaid = viewModel.state.value.accountDetail?.levelDetail?.accountType?.isPaid ?: false
        if (!isPaid) {
            val intent = HiddenNodesOnboardingActivity.createScreen(
                context = requireContext(),
                isOnboarding = false,
            )
            hiddenNodesOnboardingLauncher.launch(intent)
            activity?.overridePendingTransition(0, 0)
        } else if (isHiddenNodesOnboarded) {
            viewModel.hideOrUnhideNodes(nodeIds = nodeIds, hide = true)
        } else {
            this.tempNodeIds = nodeIds
            showHiddenNodesOnboarding()
        }
    }

    private fun showHiddenNodesOnboarding() {
        viewModel.setHiddenNodesOnboarded()

        val intent = HiddenNodesOnboardingActivity.createScreen(
            context = requireContext(),
            isOnboarding = true,
        )
        hiddenNodesOnboardingLauncher.launch(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private fun handleHiddenNodesOnboardingResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return

        viewModel.hideOrUnhideNodes(nodeIds = tempNodeIds, hide = true)

        val message = resources.getQuantityString(
            R.plurals.hidden_nodes_result_message,
            tempNodeIds.size,
            tempNodeIds.size
        )
        Util.showSnackbar(requireActivity(), message)
    }
}
