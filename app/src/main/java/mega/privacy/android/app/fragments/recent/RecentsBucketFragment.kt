package mega.privacy.android.app.fragments.recent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
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
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.adapters.MultipleBucketAdapter
import mega.privacy.android.app.utils.*
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference
import java.util.*

@AndroidEntryPoint
class RecentsBucketFragment : BaseFragment() {

    private lateinit var managerActivity: ManagerActivityLollipop

    private val args: RecentsBucketFragmentArgs by navArgs()

    private val viewModel by viewModels<RecentsBucketViewModel>()

    private lateinit var binding: FragmentRecentBucketBinding

    private lateinit var gridView: RecyclerView

    private lateinit var mAdapter: MultipleBucketAdapter

    private lateinit var bucket: BucketSaved

    private var draggingNodeHandle = MegaApiJava.INVALID_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bucket = args.bucket
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecentBucketBinding.inflate(inflater, container, false)
        gridView = binding.multipleBucketView
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        managerActivity = requireActivity() as ManagerActivityLollipop

        viewModel.serializedNodes.value = args.serializedNodes
        viewModel.items.observe(viewLifecycleOwner) {
            setupListView(it)
            setupHeaderView()
            setupFastScroller(it)
            setupToolbar()
            checkScroll()
        }
    }

    private fun setupListView(nodes: List<MegaNode>) {
        mAdapter = MultipleBucketAdapter(context, this, nodes, bucket.isMedia)
        if (bucket.isMedia) {
            val numCells: Int = if (Util.isScreenInPortrait(context)) 4 else 6
            val gridLayoutManager =
                GridLayoutManager(context, numCells, GridLayoutManager.VERTICAL, false)
            binding.multipleBucketView.layoutManager = gridLayoutManager

        } else {
            val linearLayoutManager = LinearLayoutManager(context)
            binding.multipleBucketView.layoutManager = linearLayoutManager
            binding.multipleBucketView.addItemDecoration(
                SimpleDividerItemDecoration(
                    context,
                    outMetrics
                )
            )
        }
        binding.multipleBucketView.adapter = mAdapter
        binding.multipleBucketView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
    }

    private fun setupFastScroller(nodes: List<MegaNode>) {
        if (bucket.isMedia && nodes.size >= Constants.MIN_ITEMS_SCROLLBAR) {
            binding.fastscroll.visibility = View.VISIBLE
            binding.fastscroll.setRecyclerView(binding.multipleBucketView)
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
            binding.dateText.text = TimeUtils.formatBucketDate(
                context,
                bucket.timestamp
            )
            binding.headerInfoLayout.visibility = View.VISIBLE
        }
    }

    private fun setupToolbar() {
        managerActivity.setToolbarTitle(
            "${viewModel.items.value?.size} ${getString(R.string.general_files).toUpperCase(
                Locale.ROOT
            )}"
        )
    }

    private fun checkScroll() {
        if (binding.multipleBucketView.canScrollVertically(-1)) {
            managerActivity.changeActionBarElevation(true)
        } else {
            managerActivity.changeActionBarElevation(false)
        }
    }

    private fun getNodesHandles(isImage: Boolean): LongArray? = viewModel.items.value?.filter {
        if (isImage) {
            MimeTypeList.typeForName(it.name).isImage
        } else {
            FileUtils.isAudioOrVideo(it) && FileUtils.isInternalIntent(it)
        }
    }?.map { it.handle }?.toLongArray()

    fun openFile(
        node: MegaNode,
        isMedia: Boolean,
        thumbnail: ImageView,
        openFrom: Int
    ) {
        setupDraggingThumbnailCallback()
        val screenPosition = getThumbnailLocationOnScreen(thumbnail)
        draggingNodeHandle = node.handle

        val mime = MimeTypeList.typeForName(node.name)
        when {
            mime.isImage -> {
                val intent = Intent(context, FullScreenImageViewerLollipop::class.java)
                intent.putExtra(
                    Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                    Constants.RECENTS_BUCKET_ADAPTER
                )
                intent.putExtra(
                    "screenPosition",
                    screenPosition
                )
                intent.putExtra(
                    Constants.HANDLE,
                    node.handle
                )
                intent.putExtra(
                    Constants.NODE_HANDLES,
                    getNodesHandles(isMedia)
                )
                startActivity(intent)
                managerActivity.overridePendingTransition(0, 0)
            }
        }
    }

    /** All below methods are for supporting functions of FullScreenImageViewer */

    override fun onDestroy() {
        super.onDestroy()
        FullScreenImageViewerLollipop.removeDraggingThumbnailCallback(RecentsBucketFragment::class.java)
    }

    private fun setupDraggingThumbnailCallback() {
        FullScreenImageViewerLollipop.addDraggingThumbnailCallback(
            RecentsBucketFragment::class.java,
            RecentsBucketDraggingThumbnailCallback(WeakReference(this))
        )
    }

    fun scrollToPhoto(handle: Long) {
        val position = viewModel.getItemPositionByHandle(handle)
        if (position == Constants.INVALID_POSITION) return

        gridView.scrollToPosition(position)
        notifyThumbnailLocationOnScreen()
    }

    private fun getThumbnailViewByHandle(handle: Long): ImageView? {
        val position = viewModel.getItemPositionByHandle(handle)
        val viewHolder = gridView.findViewHolderForLayoutPosition(position) ?: return null
        return viewHolder.itemView.findViewById(R.id.thumbnail_media)
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

        val intent = Intent(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG)
        intent.putExtra(Constants.INTENT_EXTRA_KEY_SCREEN_POSITION, location)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    companion object {
        private class RecentsBucketDraggingThumbnailCallback(private val fragmentRef: WeakReference<RecentsBucketFragment>) :
            DraggingThumbnailCallback {

            override fun setVisibility(v: Int) {
                val fragment = fragmentRef.get() ?: return
                fragment.getThumbnailViewByHandle(fragment.draggingNodeHandle)
                    ?.apply { visibility = v }
            }

            override fun getLocationOnScreen(location: IntArray) {
                val fragment = fragmentRef.get() ?: return
                val result = fragment.getDraggingThumbnailLocationOnScreen() ?: return
                result.copyInto(location, 0, 0, 2)
            }
        }
    }
}