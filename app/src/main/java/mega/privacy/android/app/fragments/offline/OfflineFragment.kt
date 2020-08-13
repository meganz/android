package mega.privacy.android.app.fragments.offline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.NewHeaderItemDecoration
import mega.privacy.android.app.databinding.FragmentOfflineBinding
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_GRID
import mega.privacy.android.app.lollipop.adapters.MegaNodeAdapter.ITEM_VIEW_TYPE_LIST
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ARRAY_OFFLINE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SCREEN_POSITION
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.Util.scaleHeightPx
import mega.privacy.android.app.utils.autoCleared
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import java.io.File

@AndroidEntryPoint
class OfflineFragment : Fragment() {
    private val args: OfflineFragmentArgs by navArgs()
    private var binding by autoCleared<FragmentOfflineBinding>()
    private val viewModel: OfflineViewModel by viewModels()

    private var managerActivity: ManagerActivityLollipop? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: OfflineAdapter? = null
    private var itemDecoration: NewHeaderItemDecoration? = null

    private val receiverUpdatePosition = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }

            if (intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, 0) == Constants.OFFLINE_ADAPTER) {
                val handle = intent.getLongExtra("handle", INVALID_HANDLE)
                when (intent.getIntExtra("actionType", -1)) {
                    Constants.SCROLL_TO_POSITION -> {
                        scrollToNode(handle)
                    }
                    Constants.UPDATE_IMAGE_DRAG -> {
                        hideDraggingThumbnail(handle)
                    }
                    else -> {
                    }
                }
            }
        }
    }

    private var draggingNodeHandle = INVALID_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        managerActivity = requireActivity() as ManagerActivityLollipop

        if (!args.rootFolderOnly) {
            // TODO: workaround for navigation with ManagerActivity
            managerActivity?.fullscreenOfflineFragmentOpened(this)
        }
    }

    override fun onResume() {
        super.onResume()

        instanceForDragging = null
    }

    override fun onDestroy() {
        super.onDestroy()

        if (!args.rootFolderOnly) {
            // TODO: workaround for navigation with ManagerActivity
            managerActivity?.fullscreenOfflineFragmentClosed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOfflineBinding.inflate(inflater, container, false)

        if (isList()) {
            binding.offlineBrowserList.isVisible = true
        } else {
            binding.offlineBrowserGrid.isVisible = true
        }

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            receiverUpdatePosition,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION)
        )
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setDisplayParam(
            args.rootFolderOnly, isList(),
            if (isList()) 0 else binding.offlineBrowserGrid.spanCount, args.path
        )
        setupRecyclerView()
        observeLiveData()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(receiverUpdatePosition)
    }

    private fun setupRecyclerView() {
        val rv = if (isList()) {
            binding.offlineBrowserList.layoutManager = LinearLayoutManager(context)
            binding.offlineBrowserList
        } else {
            binding.offlineBrowserGrid
        }

        adapter = OfflineAdapter(isList(), object : OfflineAdapterListener {
            override fun onNodeClicked(position: Int, node: OfflineNode) {
                viewModel.onNodeClicked(position, node)
            }

            override fun onNodeLongClicked(position: Int, node: OfflineNode) {
                viewModel.onNodeLongClicked(position, node)
            }

            override fun onOptionsClicked(position: Int, node: OfflineNode) {
                viewModel.onNodeOptionsClicked(position, node)
            }
        })
        rv.adapter = adapter
        rv.setPadding(0, 0, 0, scaleHeightPx(85, resources.displayMetrics))
        rv.clipToPadding = false
        rv.setHasFixedSize(true)
        rv.itemAnimator = DefaultItemAnimator()
        rv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })

        val decor = NewHeaderItemDecoration(context)
        if (isList()) {
            decor.setType(ITEM_VIEW_TYPE_LIST)
        } else {
            decor.setType(ITEM_VIEW_TYPE_GRID)
        }
        rv.addItemDecoration(decor)

        recyclerView = rv
        itemDecoration = decor
    }

    private fun observeLiveData() {
        viewModel.nodes.observe(viewLifecycleOwner) {
            adapter?.setNodes(it)

            val isList = isList()
            itemDecoration?.setKeys(
                viewModel.buildSectionTitle(
                    it, isList, if (isList) 0 else binding.offlineBrowserGrid.spanCount
                )
            )

            if (!args.rootFolderOnly) {
                managerActivity?.updateFullscreenOfflineFragmentOptionMenu()
            }
        }

        viewModel.actionBarTitle.observe(viewLifecycleOwner) {
            if (viewModel.selecting) {

            } else {
                managerActivity?.setToolbarTitleFromFullscreenOfflineFragment(
                    it, viewModel.path == "/"
                )
            }
        }

        viewModel.pathLiveData.observe(viewLifecycleOwner) {
            managerActivity?.pathNavigationOffline = it
        }
        viewModel.openFolderFullscreen.observe(viewLifecycleOwner) {
            managerActivity?.openFullscreenOfflineFragment(it)
        }

        viewModel.showOptionsPanel.observe(viewLifecycleOwner) {
            managerActivity?.showOptionsPanel(it)
        }
        viewModel.nodeToOpen.observe(viewLifecycleOwner) {
            openNode(it.first, it.second)
        }
        viewModel.urlFileOpenAsUrl.observe(viewLifecycleOwner) {
            logDebug("Is URL - launch browser intent")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(it)
            startActivity(intent)
        }
        viewModel.urlFileOpenAsFile.observe(viewLifecycleOwner) {
            openFile(it, MimeTypeList.typeForName(it.name))
        }
    }

    private fun openNode(position: Int, node: OfflineNode) {
        val activity = managerActivity
        if (activity != null && activity.isSearchViewExpanded && !activity.isValidSearchQuery) {
            activity.setTextSubmitted()
        }

        val file = getOfflineFile(context, node.node)
        val mime = MimeTypeList.typeForName(file.name)
        when {
            mime.isZip -> {
                logDebug("MimeTypeList ZIP")
                val intentZip = Intent(context, ZipBrowserActivityLollipop::class.java)
                intentZip.action = ZipBrowserActivityLollipop.ACTION_OPEN_ZIP_FILE
                intentZip.putExtra(
                    ZipBrowserActivityLollipop.EXTRA_ZIP_FILE_TO_OPEN,
                    viewModel.path
                )
                intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, file.absolutePath)
                startActivity(intentZip)
            }
            mime.isImage -> {
                val intent = Intent(context, FullScreenImageViewerLollipop::class.java)
                intent.putExtra("position", position)
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
                intent.putExtra("parentNodeHandle", INVALID_HANDLE)
                intent.putExtra("offlinePathDirectory", file.parent)
                val screenPosition = getThumbnailScreenPosition(position)
                if (screenPosition != null) {
                    intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
                }
                intent.putExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE, adapter!!.getOfflineNodes())

                managerActivity?.startActivity(intent)
                managerActivity?.overridePendingTransition(0, 0)
                instanceForDragging = this
                draggingNodeHandle = node.node.handle.toLong()
            }
            mime.isVideoReproducible || mime.isAudio -> {
                logDebug("Video/Audio file")

                val mediaIntent: Intent
                val internalIntent: Boolean
                var opusFile = false
                if (mime.isVideoNotSupported || mime.isAudioNotSupported) {
                    mediaIntent = Intent(Intent.ACTION_VIEW)
                    internalIntent = false
                    val s: Array<String> = file.name.split("\\.".toRegex()).toTypedArray()
                    if (s.size > 1 && s[s.size - 1] == "opus") {
                        opusFile = true
                    }
                } else {
                    internalIntent = true
                    mediaIntent = Intent(context, AudioVideoPlayerLollipop::class.java)
                }

                mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.node.handle.toLong())
                mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.node.name)
                mediaIntent.putExtra("path", file.absolutePath)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
                mediaIntent.putExtra("position", position)
                mediaIntent.putExtra("parentNodeHandle", INVALID_HANDLE)
                mediaIntent.putExtra("offlinePathDirectory", file.parent)
                val screenPosition = getThumbnailScreenPosition(position)
                if (screenPosition != null) {
                    mediaIntent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
                }
                mediaIntent.putExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE, adapter!!.getOfflineNodes())
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    mediaIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireContext(),
                            "mega.privacy.android.app.providers.fileprovider",
                            file
                        ), mime.type
                    )
                } else {
                    mediaIntent.setDataAndType(Uri.fromFile(file), mime.type)
                }
                if (opusFile) {
                    mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                }
                if (internalIntent) {
                    startActivity(mediaIntent)
                } else {
                    if (MegaApiUtils.isIntentAvailable(context, mediaIntent)) {
                        startActivity(mediaIntent)
                    } else {
                        managerActivity?.showSnackbar(
                            Constants.SNACKBAR_TYPE,
                            getString(R.string.intent_not_available),
                            -1
                        )
                        val intentShare = Intent(Intent.ACTION_SEND)
                        if (VERSION.SDK_INT >= VERSION_CODES.N) {
                            intentShare.setDataAndType(
                                FileProvider.getUriForFile(
                                    requireContext(),
                                    "mega.privacy.android.app.providers.fileprovider",
                                    file
                                ), mime.type
                            )
                        } else {
                            intentShare.setDataAndType(Uri.fromFile(file), mime.type)
                        }
                        intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                            logDebug("Call to startActivity(intentShare)")
                            startActivity(intentShare)
                        }
                    }
                }
                managerActivity?.overridePendingTransition(0, 0)
                instanceForDragging = this
                draggingNodeHandle = node.node.handle.toLong()
            }
            mime.isPdf -> {
                logDebug("PDF file")

                val pdfIntent = Intent(context, PdfViewerActivityLollipop::class.java)

                pdfIntent.putExtra(INTENT_EXTRA_KEY_INSIDE, true)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.node.handle.toLong())
                pdfIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
                pdfIntent.putExtra("path", file.absolutePath)
                pdfIntent.putExtra("pathNavigation", viewModel.path)
                val screenPosition = getThumbnailScreenPosition(position)
                if (screenPosition != null) {
                    pdfIntent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
                }
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireContext(),
                            "mega.privacy.android.app.providers.fileprovider",
                            file
                        ), mime.type
                    )
                } else {
                    pdfIntent.setDataAndType(Uri.fromFile(file), mime.type)
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(pdfIntent)
                managerActivity?.overridePendingTransition(0, 0)
                instanceForDragging = this
                draggingNodeHandle = node.node.handle.toLong()
            }
            mime.isURL -> {
                logDebug("Is URL file")
                viewModel.processUrlFile(file)
            }
            else -> {
                openFile(file, mime)
            }
        }
    }

    private fun getThumbnailScreenPosition(position: Int): IntArray? {
        val viewHolder = recyclerView?.findViewHolderForLayoutPosition(position) ?: return null
        return adapter?.getThumbnailLocationOnScreen(viewHolder)
    }

    private fun openFile(file: File, mime: MimeTypeList) {
        logDebug("openFile")
        val viewIntent = Intent(Intent.ACTION_VIEW)
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            viewIntent.setDataAndType(
                FileProvider.getUriForFile(
                    requireContext(),
                    "mega.privacy.android.app.providers.fileprovider",
                    file
                ), mime.type
            )
        } else {
            viewIntent.setDataAndType(Uri.fromFile(file), mime.type)
        }
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (MegaApiUtils.isIntentAvailable(context, viewIntent)) {
            startActivity(viewIntent)
        } else {
            val intentShare = Intent(Intent.ACTION_SEND)
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                intentShare.setDataAndType(
                    FileProvider.getUriForFile(
                        requireContext(),
                        "mega.privacy.android.app.providers.fileprovider",
                        file
                    ), mime.type
                )
            } else {
                intentShare.setDataAndType(
                    Uri.fromFile(file),
                    MimeTypeList.typeForName(file.name).type
                )
            }
            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                startActivity(intentShare)
            }
        }
    }

    private fun isList(): Boolean {
        return managerActivity?.isList ?: true || args.rootFolderOnly
    }

    fun checkScroll() {
        val rv = recyclerView
        if (rv != null) {
            managerActivity?.changeActionBarElevation(
                rv.canScrollVertically(-1) || viewModel.selecting
            )
        }
    }

    fun setOrder(order: Int) {
        viewModel.setOrder(order)
    }

    fun onBackPressed(): Int {
        return viewModel.navigateOut()
    }

    fun getItemCount(): Int {
        return adapter?.itemCount ?: 0
    }

    fun scrollToNode(handle: Long) {
        logDebug("scrollToNode, handle $handle")

        val position = adapter?.getNodePosition(handle) ?: return
        logDebug("scrollToNode, handle $handle, position $position")
        if (position != INVALID_POSITION) {
            recyclerView?.scrollToPosition(position)
            notifyThumbnailLocationOnScreen()
        }
    }

    fun hideDraggingThumbnail(handle: Long) {
        logDebug("hideDraggingThumbnail: $handle")
        setDraggingThumbnailVisibility(draggingNodeHandle, View.VISIBLE)
        setDraggingThumbnailVisibility(handle, View.GONE)
        draggingNodeHandle = handle
        notifyThumbnailLocationOnScreen()
    }

    private fun setDraggingThumbnailVisibility(
        handle: Long,
        visibility: Int
    ) {
        val position = adapter?.getNodePosition(handle) ?: return
        val viewHolder: ViewHolder =
            recyclerView?.findViewHolderForLayoutPosition(position) ?: return
        adapter?.setThumbnailVisibility(viewHolder, visibility)
    }

    private fun notifyThumbnailLocationOnScreen() {
        val position = adapter?.getNodePosition(draggingNodeHandle) ?: return
        val viewHolder: ViewHolder =
            recyclerView?.findViewHolderForLayoutPosition(position) ?: return
        val res = adapter?.getThumbnailLocationOnScreen(viewHolder) ?: return
        res[0] += res[2] / 2
        res[1] += res[3] / 2
        val intent = Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, res)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    companion object {
        var instanceForDragging: OfflineFragment? = null

        fun setArgs(fragment: OfflineFragment, rootFolderOnly: Boolean) {
            fragment.arguments = OfflineFragmentArgs("/", rootFolderOnly).toBundle()
        }

        /**
         * Get the location on screen of the dragging node thumbnail.
         *
         * TODO: we need figure out a better way to implement the drag feature, this kind of static hack
         * should be avoided.
         *
         * @param location out param for location
         */
        @JvmStatic
        fun getDraggingThumbnailLocationOnScreen(location: IntArray) {
            val fragment = instanceForDragging ?: return
            val position = fragment.adapter?.getNodePosition(fragment.draggingNodeHandle) ?: return
            val viewHolder: ViewHolder =
                fragment.recyclerView?.findViewHolderForLayoutPosition(position) ?: return
            val res = fragment.adapter?.getThumbnailLocationOnScreen(viewHolder) ?: return
            System.arraycopy(res, 0, location, 0, 2)
        }

        @JvmStatic
        fun setDraggingThumbnailVisibility(visibility: Int) {
            val fragment = instanceForDragging ?: return
            fragment.setDraggingThumbnailVisibility(fragment.draggingNodeHandle, visibility)
        }

    }
}
