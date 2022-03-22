package mega.privacy.android.app.imageviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.*
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
import com.facebook.drawee.backends.pipeline.Fresco
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityImageViewerBinding
import mega.privacy.android.app.imageviewer.adapter.ImageViewerAdapter
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.dialog.ImageBottomSheetDialogFragment
import mega.privacy.android.app.imageviewer.util.*
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.ContextUtils.isLowMemory
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaNode

/**
 * Entry point to show an image based on an existing Node.
 */
@AndroidEntryPoint
class ImageViewerActivity : BaseActivity(), PermissionRequester, SnackbarShower {

    companion object {
        private const val IMAGE_OFFSCREEN_PAGE_LIMIT = 2

        /**
         * Get Image Viewer intent to show a single image node.
         *
         * @param context       Required to build the Intent.
         * @param nodeHandle    Node handle to request image from.
         * @return              Image Viewer Intent.
         */
        @JvmStatic
        fun getIntentForSingleNode(
            context: Context,
            nodeHandle: Long
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            }

        /**
         * Get Image Viewer intent to show a single image node.
         *
         * @param context       Required to build the Intent.
         * @param nodeFileLink  Public link to a file in MEGA.
         * @return              Image Viewer Intent.
         */
        @JvmStatic
        fun getIntentForSingleNode(
            context: Context,
            nodeFileLink: String
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_LINK, nodeFileLink)
            }

        /**
         * Get Image Viewer intent to show a single image node.
         *
         * @param context       Required to build the Intent.
         * @param nodeHandle    Offline node handle to request image from.
         * @return              Image Viewer Intent.
         */
        @JvmStatic
        fun getIntentForSingleOfflineNode(
            context: Context,
            nodeHandle: Long
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_OFFLINE_HANDLE, nodeHandle)
            }

        /**
         * Get Image Viewer intent to show a list of image nodes.
         *
         * @param context           Required to build the Intent.
         * @param parentNodeHandle  Parent node to retrieve every other child.
         * @param childOrder        Node search order.
         * @param currentNodeHandle Current node handle to show.
         * @return                  Image Viewer Intent.
         */
        @JvmStatic
        @JvmOverloads
        fun getIntentForParentNode(
            context: Context,
            parentNodeHandle: Long,
            childOrder: Int = ORDER_PHOTO_ASC,
            currentNodeHandle: Long? = null
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentNodeHandle)
                putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, childOrder)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
            }

        /**
         * Get Image Viewer intent to show a list of image nodes.
         *
         * @param context           Required to build the Intent.
         * @param childrenHandles   Child image nodes to retrieve.
         * @param currentNodeHandle Current node handle to show.
         * @return                  Image Viewer Intent.
         */
        @JvmStatic
        @JvmOverloads
        fun getIntentForChildren(
            context: Context,
            childrenHandles: LongArray,
            currentNodeHandle: Long? = null
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(NODE_HANDLES, childrenHandles)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
            }

        /**
         * Get Image Viewer intent to show a list of image nodes from chat messages.
         *
         * @param context           Required to build the Intent.
         * @param messageIds        Message Ids to be retrieved.
         * @param chatRoomId        Chat Room Id of given messages.
         * @param currentNodeHandle Current node handle to show.
         * @return                  Image Viewer Intent.
         */
        @JvmStatic
        @JvmOverloads
        fun getIntentForChatMessages(
            context: Context,
            chatRoomId: Long,
            messageIds: LongArray,
            currentNodeHandle: Long? = null
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_CHAT_ID, chatRoomId)
                putExtra(INTENT_EXTRA_KEY_MSG_ID, messageIds)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
            }

        /**
         * Get Image Viewer intent to show a list of image nodes.
         *
         * @param context           Required to build the Intent.
         * @param childrenHandles   Offline child image nodes to retrieve.
         * @param currentNodeHandle Current node handle to show.
         * @return                  Image Viewer Intent.
         */
        @JvmStatic
        fun getIntentForOfflineChildren(
            context: Context,
            childrenHandles: LongArray,
            currentNodeHandle: Long? = null
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE, childrenHandles)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
            }

        /**
         * Get Image Viewer intent to show image files.
         *
         * @param context           Required to build the Intent.
         * @param imageFileUri      Image file uri to be shown.
         * @param showNearbyFiles   Show nearby files from current parent file.
         * @return                  Image Viewer Intent.
         */
        @JvmStatic
        fun getIntentForFile(
            context: Context,
            imageFileUri: Uri,
            showNearbyFiles: Boolean = false
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_URI, imageFileUri)
                putExtra(INTENT_EXTRA_KEY_SHOW_NEARBY_FILES, showNearbyFiles)
            }
    }

    private val nodeHandle: Long? by extra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
    private val nodeOfflineHandle: Long? by extra(INTENT_EXTRA_KEY_OFFLINE_HANDLE, INVALID_HANDLE)
    private val parentNodeHandle: Long? by extra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
    private val nodeFileLink: String? by extra(EXTRA_LINK)
    private val childrenHandles: LongArray? by extra(NODE_HANDLES)
    private val childrenOfflineHandles: LongArray? by extra(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
    private val childOrder: Int? by extra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN)
    private val chatRoomId: Long? by extra(INTENT_EXTRA_KEY_CHAT_ID)
    private val chatMessagesId: LongArray? by extra(INTENT_EXTRA_KEY_MSG_ID)
    private val imageFileUri: Uri? by extra(INTENT_EXTRA_KEY_URI)
    private val showNearbyFiles: Boolean? by extra(INTENT_EXTRA_KEY_SHOW_NEARBY_FILES)

    private val viewModel by viewModels<ImageViewerViewModel>()
    private val pagerAdapter by lazy { ImageViewerAdapter(this) }
    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateCurrentPosition(position, false)
            }
        }
    }

    private var pageCallbackSet = false
    private var bottomSheet: ImageBottomSheetDialogFragment? = null
    private var nodeSaver: NodeSaver? = null
    private var nodeAttacher: MegaAttacher? = null
    private var dragToExit: DragToExitSupport? = null

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupAttachers(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(dragToExit?.wrapContentView(binding.root) ?: binding.root)

        setupView()
        setupObservers(savedInstanceState == null)

        if (savedInstanceState == null) {
            if (!Fresco.hasBeenInitialized()) Fresco.initialize(this)
            binding.root.post {
                dragToExit?.runEnterAnimation(intent, binding.root) { startAnimation ->
                    changeToolbarVisibility(!startAnimation, true)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image_viewer, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver?.saveState(outState)
        nodeAttacher?.saveState(outState)
    }

    override fun onLowMemory() {
        logWarning("onLowMemory")
        binding.viewPager.offscreenPageLimit = OFFSCREEN_PAGE_LIMIT_DEFAULT
        Fresco.getImagePipeline().clearMemoryCaches()
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        if (isFinishing) dragToExit?.showPreviousHiddenThumbnail()
        dragToExit = null
        nodeSaver?.destroy()
        nodeSaver = null
        nodeAttacher = null
        super.onDestroy()
    }

    @SuppressLint("WrongConstant")
    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.viewPager.apply {
            isSaveEnabled = false
            offscreenPageLimit = if (isLowMemory()) OFFSCREEN_PAGE_LIMIT_DEFAULT else IMAGE_OFFSCREEN_PAGE_LIMIT
            setPageTransformer(MarginPageTransformer(resources.getDimensionPixelSize(R.dimen.image_viewer_pager_margin)))
            adapter = pagerAdapter
        }

        binding.root.post {
            // Apply system bars top and bottom insets
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.toolbar.updatePadding(0, insets.top, 0, 0)
                binding.motion.updatePadding(insets.left, 0, insets.right, insets.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers(requestImagesData: Boolean) {
        if (requestImagesData) {
            when {
                parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE ->
                    viewModel.retrieveImagesFromParent(parentNodeHandle!!, childOrder, nodeHandle)
                childrenHandles?.isNotEmpty() == true ->
                    viewModel.retrieveImages(childrenHandles!!, nodeHandle)
                childrenOfflineHandles?.isNotEmpty() == true ->
                    viewModel.retrieveImages(childrenOfflineHandles!!, nodeHandle, isOffline = true)
                nodeOfflineHandle != null && nodeOfflineHandle != INVALID_HANDLE ->
                    viewModel.retrieveSingleImage(nodeOfflineHandle!!, isOffline = true)
                chatRoomId != null && chatMessagesId?.isNotEmpty() == true ->
                    viewModel.retrieveChatImages(chatRoomId!!, chatMessagesId!!, nodeHandle)
                !nodeFileLink.isNullOrBlank() ->
                    viewModel.retrieveSingleImage(nodeFileLink!!)
                nodeHandle != null && nodeHandle != INVALID_HANDLE ->
                    viewModel.retrieveSingleImage(nodeHandle!!)
                imageFileUri != null -> {
                    val fakeHandle = FileUtil.getFileFakeHandle(imageFileUri!!.toFile())
                    viewModel.retrieveFileImage(imageFileUri!!, showNearbyFiles, fakeHandle)
                }
                else ->
                    error("Invalid params")
            }
        }

        viewModel.onImagesHandle().observe(this) { items ->
            if (items.isNullOrEmpty()) {
                logError("Null or empty image items")
                finish()
            } else {
                binding.viewPager.waitForLayout {
                    val sizeDifference = pagerAdapter.itemCount != items.size
                    pagerAdapter.submitList(items) {
                        if (sizeDifference) {
                            pagerAdapter.notifyDataSetChanged()
                        }
                    }
                    true
                }
            }
            binding.progress.hide()
        }
        viewModel.onCurrentImageItem().observe(this, ::showCurrentImageInfo)
        viewModel.onShowToolbar().observe(this, ::changeToolbarVisibility)
        viewModel.onSnackbarMessage().observe(this) { message ->
            bottomSheet?.dismissAllowingStateLoss()
            showSnackbar(message)
        }
        viewModel.onCurrentPosition().observe(this) { positionPair ->
            binding.txtPageCount.apply {
                text = StringResourcesUtils.getString(
                    R.string.wizard_steps_indicator,
                    positionPair.first + 1,
                    positionPair.second
                )
                isVisible = positionPair.second > 1
            }

            binding.viewPager.apply {
                waitForLayout {
                    if (currentItem != positionPair.first) {
                        setCurrentItem(positionPair.first, false)
                    }

                    if (!pageCallbackSet) {
                        pageCallbackSet = true
                        registerOnPageChangeCallback(pageChangeCallback)
                    }
                    true
                }
            }
        }
    }

    private fun setupAttachers(savedInstanceState: Bundle?) {
        dragToExit = DragToExitSupport(this, { changeToolbarVisibility(!it, true) }) {
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }

        nodeAttacher = MegaAttacher(this).apply {
            savedInstanceState?.let(::restoreState)
        }

        nodeSaver = NodeSaver(
            this, this, this,
            showSaveToDeviceConfirmDialog(this)
        ).apply {
            savedInstanceState?.let(::restoreState)
        }
    }

    /**
     * Populate current image information to bottom texts and toolbar options.
     *
     * @param imageItem  Image item to show
     */
    private fun showCurrentImageInfo(imageItem: ImageItem?) {
        if (imageItem?.nodeItem != null) {
            binding.txtTitle.text = imageItem.nodeItem.name
            binding.toolbar.menu?.apply {
                findItem(R.id.action_forward)?.isVisible = imageItem.shouldShowForwardOption()
                findItem(R.id.action_share)?.isVisible = imageItem.isFromChat() && imageItem.shouldShowShareOption()
                findItem(R.id.action_download)?.isVisible = imageItem.shouldShowDownloadOption()
                findItem(R.id.action_get_link)?.isVisible = imageItem.shouldShowManageLinkOption()
                findItem(R.id.action_send_to_chat)?.isVisible = imageItem.shouldShowSendToContactOption(viewModel.isUserLoggedIn())
                findItem(R.id.action_more)?.isVisible = imageItem.nodeItem.handle != INVALID_HANDLE
            }
        } else {
            logWarning("Null MegaNodeItem")
        }
    }

    /**
     * Change toolbar/bottomBar visibility with animation.
     *
     * @param show                  Show or hide toolbar/bottombar
     * @param enableTransparency    Enable transparency change
     */
    private fun changeToolbarVisibility(show: Boolean, enableTransparency: Boolean = false) {
        binding.motion.post {
            val color: Int
            if (show) {
                color = R.color.white_black
                binding.motion.transitionToEnd()
            } else {
                color = android.R.color.transparent
                binding.motion.transitionToStart()
            }
            if (enableTransparency) {
                binding.motion.setBackgroundColor(ContextCompat.getColor(this, color))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val imageItem = viewModel.getCurrentImageItem() ?: return true
        val nodeItem = imageItem.nodeItem ?: return true

        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_forward -> {
                nodeItem.node?.let(::attachNode)
                true
            }
            R.id.action_share -> {
                when {
                    imageItem.isOffline ->
                        OfflineUtils.shareOfflineNode(this, nodeItem.handle)
                    imageItem.imageResult?.fullSizeUri?.toFile()?.exists() == true ->
                        FileUtil.shareFile(this, imageItem.imageResult.fullSizeUri!!.toFile())
                    !imageItem.nodePublicLink.isNullOrBlank() ->
                        MegaNodeUtil.shareLink(this, imageItem.nodePublicLink)
                    imageItem.nodeItem.node != null ->
                        viewModel.exportNode(imageItem.nodeItem.node).observe(this) { link ->
                            if (!link.isNullOrBlank()) {
                                MegaNodeUtil.shareLink(this, link)
                            }
                        }
                    else ->
                        logWarning("Node cannot be shared")
                }
                true
            }
            R.id.action_download -> {
                if (nodeItem.isAvailableOffline) {
                    saveOfflineNode(nodeItem.handle)
                } else if (nodeItem.node != null) {
                    saveNode(nodeItem.node)
                }
                true
            }
            R.id.action_get_link -> {
                LinksUtil.showGetLinkActivity(this, nodeItem.handle)
                true
            }
            R.id.action_send_to_chat -> {
                nodeItem.node?.let(::attachNode)
                true
            }
            R.id.action_more -> {
                bottomSheet = ImageBottomSheetDialogFragment.newInstance(nodeItem.handle).apply {
                    show(supportFragmentManager)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun saveNode(node: MegaNode) {
        nodeSaver?.saveNode(
            node,
            highPriority = false,
            isFolderLink = node.isForeign,
            fromMediaViewer = true,
            needSerialize = true
        )
    }

    fun saveOfflineNode(nodeHandle: Long) {
        nodeSaver?.saveOfflineNode(nodeHandle, true)
    }

    fun attachNode(node: MegaNode) {
        nodeAttacher?.attachNode(node)
    }

    fun launchVideoScreen(imageItem: ImageItem) {
        val nodeHandle = imageItem.handle
        val nodeName = imageItem.nodeItem?.name ?: return

        val intent = Util.getMediaIntent(this, nodeName).apply {
            putExtra(INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_IMAGE_VIEWER)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val existingFile = imageItem.imageResult?.fullSizeUri?.toFile()
        if (existingFile?.exists() == true && existingFile.canRead()) {
            val localPath = existingFile.absolutePath ?: return
            FileUtil.setLocalIntentParams(this, nodeName, intent, localPath, false, this)
        } else {
            val node = imageItem.nodeItem.node ?: return
            val localPath = FileUtil.getLocalFile(node)
            if (FileUtil.isLocalFile(node, megaApi, localPath)) {
                FileUtil.setLocalIntentParams(this, nodeName, intent, localPath, false, this)
            } else {
                FileUtil.setStreamingIntentParams(this, node, megaApi, intent, this)
            }
        }

        startActivity(intent)
    }

    fun showRenameDialog(node: MegaNode) {
        showRenameNodeDialog(this, node, this, null)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        nodeSaver?.handleRequestPermissionsResult(requestCode)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @Suppress("deprecation")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        when {
            nodeSaver?.handleActivityResult(this, requestCode, resultCode, intent) == true ->
                return
            nodeAttacher?.handleActivityResult(requestCode, resultCode, intent, this) == true ->
                return
            else ->
                super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.root, content, chatId)
    }

    override fun shouldSetStatusBarTextColor(): Boolean = false
}
