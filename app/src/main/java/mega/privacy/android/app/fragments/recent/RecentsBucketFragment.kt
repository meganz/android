package mega.privacy.android.app.fragments.recent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ash.TL
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
import mega.privacy.android.app.lollipop.managerSections.RecentsFragment
import mega.privacy.android.app.utils.*
import nz.mega.sdk.MegaNode
import java.util.*

@AndroidEntryPoint
class RecentsBucketFragment : BaseFragment() {

    private var managerActivity: ManagerActivityLollipop? = null

    private val args: RecentsBucketFragmentArgs by navArgs()

    private var binding by autoCleared<FragmentRecentBucketBinding>()

    private var divider: SimpleDividerItemDecoration? = null

    private lateinit var nodes: List<MegaNode>

    private lateinit var bucket: BucketSaved

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nodes = args.serializedNodes.map { MegaNode.unserialize(it) }
        bucket = args.bucket
        managerActivity = requireActivity() as ManagerActivityLollipop
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRecentBucketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListView()
        setupHeaderView()
        setupFastScroller()
        setupToolbar()
        checkScroll()
    }

    private fun setupListView() {
        val multipleBucketAdapter = MultipleBucketAdapter(context, this, nodes, bucket.isMedia)
        if (bucket.isMedia) {
            val numCells: Int = if (Util.isScreenInPortrait(context)) 4 else 6
            val gridLayoutManager =
                GridLayoutManager(context, numCells, GridLayoutManager.VERTICAL, false)
            binding.multipleBucketView.layoutManager = gridLayoutManager
            if (divider != null) {
                binding.multipleBucketView.removeItemDecoration(divider!!)
            }
        } else {
            val linearLayoutManager = LinearLayoutManager(context)
            binding.multipleBucketView.layoutManager = linearLayoutManager
            if (divider == null) {
                divider = SimpleDividerItemDecoration(context, outMetrics)
            }
            binding.multipleBucketView.addItemDecoration(divider!!)
        }
        binding.multipleBucketView.adapter = multipleBucketAdapter
        binding.multipleBucketView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
    }

    private fun setupFastScroller() {
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
        managerActivity?.setToolbarTitle(
            "${nodes.size} ${getString(R.string.general_files).toUpperCase(
                Locale.ROOT
            )}"
        )
    }

    private fun checkScroll() {
        if (binding.multipleBucketView.canScrollVertically(-1)) {
            managerActivity?.changeActionBarElevation(true)
        } else {
            managerActivity?.changeActionBarElevation(false)
        }
    }

    private fun getNodesHandles(isImage: Boolean): LongArray = nodes.filter {
        if (isImage) {
            MimeTypeList.typeForName(it.name).isImage
        } else {
            FileUtils.isAudioOrVideo(it) && FileUtils.isInternalIntent(it)
        }
    }.map { it.handle }.toLongArray()

    companion object {

        @JvmStatic
        var imageDrag: ImageView? = null

        @JvmStatic
        fun setDraggingThumbnailVisibility(visibility : Int) {
            imageDrag?.visibility = visibility
        }

        @JvmStatic
        fun getDraggingThumbnailLocationOnScreen(location: IntArray) {
            imageDrag?.getLocationOnScreen(location)
        }
    }

    fun openFile(
        node: MegaNode,
        isMedia: Boolean,
        thumbnail: ImageView?,
        openFrom: Int
    ) {
        TL.log(node.name)
        imageDrag = thumbnail

        var screenPosition: IntArray? = null
        if (thumbnail != null) {
            screenPosition = IntArray(4)
            val loc = IntArray(2)
            thumbnail.getLocationOnScreen(loc)
            screenPosition[0] = loc[0]
            screenPosition[1] = loc[1]
            screenPosition[2] = thumbnail.width
            screenPosition[3] = thumbnail.height
        }

        val mime = MimeTypeList.typeForName(node.name)

        when {
            mime.isImage -> {
                val intent = Intent(context, FullScreenImageViewerLollipop::class.java)
                intent.putExtra(
                    Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                    Constants.RECENTS_BUCKET_ADAPTER
                )
                if (screenPosition != null) {
                    intent.putExtra(
                        "screenPosition",
                        screenPosition
                    )
                }
                intent.putExtra(
                    Constants.HANDLE,
                    node.handle
                )
                intent.putExtra(
                    Constants.NODE_HANDLES,
                    getNodesHandles(isMedia)
                )
                startActivity(intent)
                managerActivity?.overridePendingTransition(0, 0)
            }
        }

    }

}