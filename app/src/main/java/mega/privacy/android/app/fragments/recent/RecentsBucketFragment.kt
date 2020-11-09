package mega.privacy.android.app.fragments.recent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import mega.privacy.android.app.databinding.FragmentRecentBucketBinding
import mega.privacy.android.app.fragments.BaseFragment
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop
import mega.privacy.android.app.lollipop.adapters.MultipleBucketAdapter
import mega.privacy.android.app.lollipop.controllers.NodeController
import mega.privacy.android.app.utils.*
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logDebug
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference
import java.util.*

@AndroidEntryPoint
class RecentsBucketFragment : BaseFragment() {

    private val viewModel by viewModels<RecentsBucketViewModel>()

    private val selectedBucketModel: SelectedBucketViewModel by activityViewModels()

    private lateinit var binding: FragmentRecentBucketBinding

    private lateinit var listView: RecyclerView

    private lateinit var mAdapter: MultipleBucketAdapter

    private lateinit var bucket: BucketSaved

    private var draggingNodeHandle = MegaApiJava.INVALID_HANDLE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        setupDraggingThumbnailCallback()
    }

    private fun setupListView(nodes: List<MegaNode>) {
        mAdapter = MultipleBucketAdapter(activity, this, nodes, bucket.isMedia)

        if (bucket.isMedia) {
            val numCells: Int = if (Util.isScreenInPortrait(activity)) 4 else 6
            val gridLayoutManager =
                GridLayoutManager(activity, numCells, GridLayoutManager.VERTICAL, false)

            listView.layoutManager = gridLayoutManager
        } else {
            val linearLayoutManager = LinearLayoutManager(activity)

            listView.layoutManager = linearLayoutManager
            listView.addItemDecoration(SimpleDividerItemDecoration(activity, outMetrics))
        }

        listView.adapter = mAdapter
        listView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })

        listView.clipToPadding = false
        listView.setHasFixedSize(true)
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

            if (bucket.isUpdate) {
                binding.actionImage.setImageResource(R.drawable.ic_versions_small)
            } else {
                binding.actionImage.setImageResource(R.drawable.ic_recents_up)
            }

            binding.dateText.text =
                TimeUtils.formatBucketDate(activity, bucket.timestamp)
            binding.headerInfoLayout.visibility = View.VISIBLE
        }
    }

    private fun setupToolbar() {
        (activity as ManagerActivityLollipop).setToolbarTitle(
            "${viewModel.items.value?.size} ${getString(R.string.general_files).toUpperCase(Locale.ROOT)}"
        )
    }

    private fun checkScroll() {
        val canScroll = listView.canScrollVertically(-1)
        (activity as ManagerActivityLollipop).changeActionBarElevation(canScroll)
    }

    private fun getNodesHandles(isImage: Boolean): LongArray? = viewModel.items.value?.filter {
        if (isImage) {
            MimeTypeList.typeForName(it.name).isImage
        } else {
            FileUtil.isAudioOrVideo(it) && FileUtil.isInternalIntent(it)
        }
    }?.map { it.handle }?.toLongArray()

    fun openFile(
        node: MegaNode,
        isMedia: Boolean,
        thumbnail: ImageView
    ) {
        setupDraggingThumbnailCallback()
        val screenPosition = getThumbnailLocationOnScreen(thumbnail)
        draggingNodeHandle = node.handle

        val mime = MimeTypeList.typeForName(node.name)
        val localPath =
            FileUtil.getLocalFile(activity, node.name, node.size)
        logDebug("Open node: ${node.name} which mime is: ${mime.type}, local path is: $localPath")

        when {
            mime.isImage -> {
                openImage(screenPosition, node)
            }
            FileUtil.isAudioOrVideo(node) -> {
                openAudioVideo(screenPosition, node, isMedia, localPath)
            }
            mime.isURL -> {
                openURL(node, localPath)
            }
            mime.isPdf -> {
                openPdf(node, localPath)
            }
            else -> {
                download(node.handle)
            }
        }
    }

    private fun openPdf(
        node: MegaNode,
        localPath: String?
    ) {
        val intent = Intent(context, PdfViewerActivityLollipop::class.java)
        intent.putExtra(INTENT_EXTRA_KEY_INSIDE, true)
        intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_BUCKET_ADAPTER)

        val paramsSetSuccessfully =
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(activity, node, intent, localPath, false)
            } else {
                FileUtil.setStreamingIntentParams(activity, node, megaApi, intent)
            }
        intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.handle)
        openOrDownload(intent, paramsSetSuccessfully, node.handle)
    }

    private fun openURL(
        node: MegaNode,
        localPath: String?
    ) {
        val intent = Intent(Intent.ACTION_VIEW)
        val paramsSetSuccessfully =
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setURLIntentParams(context, node, intent, localPath)
            } else false

        openOrDownload(intent, paramsSetSuccessfully, node.handle)
    }

    private fun openAudioVideo(
        screenPosition: IntArray,
        node: MegaNode,
        isMedia: Boolean,
        localPath: String?
    ) {
        val intent = if (FileUtil.isInternalIntent(node)) {
            Intent(activity, AudioVideoPlayerLollipop::class.java)
        } else {
            Intent(Intent.ACTION_VIEW)
        }

        intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_BUCKET_ADAPTER)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
        intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.name)

        if (isMedia) {
            intent.putExtra(NODE_HANDLES, getNodesHandles(false))
            intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true)
        } else {
            intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false)
        }

        val paramsSetSuccessfully =
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(activity, node, intent, localPath, false)
            } else {
                FileUtil.setStreamingIntentParams(activity, node, megaApi, intent)
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
        handle: Long
    ) {
        if (paramsSetSuccessfully && MegaApiUtils.isIntentAvailable(activity, intent)) {
            activity?.startActivity(intent)
            activity?.overridePendingTransition(0, 0)
        } else {
            (activity as ManagerActivityLollipop).showSnackbar(
                SNACKBAR_TYPE,
                getString(R.string.intent_not_available),
                MEGACHAT_INVALID_HANDLE
            )
            download(handle)
        }
    }

    private fun openImage(
        screenPosition: IntArray,
        node: MegaNode
    ) {
        val intent = Intent(activity, FullScreenImageViewerLollipop::class.java)

        intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_BUCKET_ADAPTER)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
        intent.putExtra(HANDLE, node.handle)
        intent.putExtra(NODE_HANDLES, getNodesHandles(true))

        startActivity(intent)
        activity?.overridePendingTransition(0, 0)
    }

    private fun download(handle: Long) {
        val handleList = ArrayList<Long>()
        handleList.add(handle)
        val nC = NodeController(activity)
        nC.prepareForDownload(handleList, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        FullScreenImageViewerLollipop.removeDraggingThumbnailCallback(RecentsBucketFragment::class.java)
        AudioVideoPlayerLollipop.removeDraggingThumbnailCallback(RecentsBucketFragment::class.java)
    }

    /** All below methods are for supporting functions of FullScreenImageViewer */

    private fun setupDraggingThumbnailCallback() {
        FullScreenImageViewerLollipop.addDraggingThumbnailCallback(
            RecentsBucketFragment::class.java,
            RecentsBucketDraggingThumbnailCallback(WeakReference(this))
        )

        AudioVideoPlayerLollipop.addDraggingThumbnailCallback(
            RecentsBucketFragment::class.java,
            RecentsBucketDraggingThumbnailCallback(WeakReference(this))
        )
    }

    fun scrollToPosition(handle: Long) {
        val position = viewModel.getItemPositionByHandle(handle)
        if (position == INVALID_POSITION) return

        listView.scrollToPosition(position)
        notifyThumbnailLocationOnScreen()
    }

    private fun getThumbnailViewByHandle(handle: Long): ImageView? {
        val position = viewModel.getItemPositionByHandle(handle)
        val viewHolder = listView.findViewHolderForLayoutPosition(position) ?: return null

        // List and grid have different thumnail ImageView
        return if (bucket.isMedia) {
            viewHolder.itemView.findViewById(R.id.thumbnail_media)
        } else {
            viewHolder.itemView.findViewById(R.id.thumbnail_list)
        }
    }

    private fun getThumbnailLocationOnScreen(imageView: ImageView): IntArray {
        val topLeft = IntArray(2)

        imageView.getLocationOnScreen(topLeft)
        return intArrayOf(topLeft[0], topLeft[1], imageView.width, imageView.height)
    }

    private fun getDraggingThumbnailLocationOnScreen(): IntArray? {
        val thumbnailView = getThumbnailViewByHandle(draggingNodeHandle) ?: return null
        return getThumbnailLocationOnScreen(thumbnailView)
    }

    fun hideDraggingThumbnail(handle: Long) {
        getThumbnailViewByHandle(draggingNodeHandle)?.apply { visibility = View.VISIBLE }
        getThumbnailViewByHandle(handle)?.apply { visibility = View.INVISIBLE }
        draggingNodeHandle = handle
        notifyThumbnailLocationOnScreen()
    }

    private fun notifyThumbnailLocationOnScreen() {
        val location = getDraggingThumbnailLocationOnScreen() ?: return
        location[0] += location[2] / 2
        location[1] += location[3] / 2

        val intent = Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, location)
        context.sendBroadcast(intent)
    }

    companion object {
        private class RecentsBucketDraggingThumbnailCallback(private val fragmentRef: WeakReference<RecentsBucketFragment>) :
            DraggingThumbnailCallback {

            override fun setVisibility(visibility: Int) {
                val fragment = fragmentRef.get() ?: return
                fragment.getThumbnailViewByHandle(fragment.draggingNodeHandle)
                    ?.apply { this.visibility = visibility }
            }

            override fun getLocationOnScreen(location: IntArray) {
                val fragment = fragmentRef.get() ?: return
                val result = fragment.getDraggingThumbnailLocationOnScreen() ?: return
                result.copyInto(location, 0, 0, 2)
            }
        }
    }
}