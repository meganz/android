package mega.privacy.android.app.imageviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import com.facebook.drawee.backends.pipeline.Fresco
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.analytics.event.link.LinkShareLinkTapFileButtonInfo
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.dragger.DragToExitSupport
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityImageViewerBinding
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.dialog.ImageBottomSheetDialogFragment
import mega.privacy.android.app.imageviewer.util.shouldShowDownloadOption
import mega.privacy.android.app.imageviewer.util.shouldShowForwardOption
import mega.privacy.android.app.imageviewer.util.shouldShowManageLinkOption
import mega.privacy.android.app.imageviewer.util.shouldShowSendToContactOption
import mega.privacy.android.app.imageviewer.util.shouldShowShareOption
import mega.privacy.android.app.imageviewer.util.shouldShowSlideshowOption
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.interfaces.showTransfersSnackBar
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.EXTRA_LINK
import mega.privacy.android.app.utils.Constants.FROM_IMAGE_VIEWER
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ARRAY_OFFLINE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_FILE_VERSION
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SHOW_NEARBY_FILES
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_URI
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.permission.PermissionUtils
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaNode
import timber.log.Timber
import javax.inject.Inject

/**
 * Entry point to show an image based on an existing Node.
 */
@AndroidEntryPoint
class ImageViewerActivity : BaseActivity(), PermissionRequester, SnackbarShower {

    companion object {
        const val IMAGE_OFFSCREEN_PAGE_LIMIT = 2
        private const val EXTRA_SHOW_SLIDESHOW = "EXTRA_SHOW_SLIDESHOW"
        private const val EXTRA_IS_TIMELINE = "EXTRA_IS_TIMELINE"

        /**
         * Get Image Viewer intent to show a single image node.
         *
         * @param context       Required to build the Intent.
         * @param nodeHandle    Node handle to request image from.
         * @param isFileVersion True if is a file version, false otherwise.
         * @return              Image Viewer Intent.
         */
        @JvmStatic
        fun getIntentForSingleNode(
            context: Context,
            nodeHandle: Long,
            isFileVersion: Boolean = false,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
                putExtra(INTENT_EXTRA_KEY_IS_FILE_VERSION, isFileVersion)
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
            nodeFileLink: String,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_LINK, nodeFileLink)
            }

        /**
         * Get Image Viewer intent for reordering activity back to front.
         * Caution: Only call this intent if image viewer is already running, otherwise no node handle.
         *
         * @param context       Required to build the Intent.
         * @return              Image Viewer Intent.
         */
        @JvmStatic
        fun getIntentFromBackStack(
            context: Context,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
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
            nodeHandle: Long,
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
            parentNodeHandle: Long?,
            childOrder: SortOrder = SortOrder.ORDER_PHOTO_ASC,
            currentNodeHandle: Long? = null,
            showSlideshow: Boolean = false,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, parentNodeHandle)
                putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, childOrder)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
                putExtra(EXTRA_SHOW_SLIDESHOW, showSlideshow)
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
            currentNodeHandle: Long? = null,
            showSlideshow: Boolean = false,
            fromFolderLink: Boolean = false,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(NODE_HANDLES, childrenHandles)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
                putExtra(EXTRA_SHOW_SLIDESHOW, showSlideshow)
                if (fromFolderLink)
                    putExtra(
                        Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
                        Constants.FOLDER_LINK_ADAPTER //FolderLink type
                    )
            }

        /**
         * Get Image Viewer intent to show Timeline image nodes.
         *
         * @param context           Required to build the Intent.
         * @param childOrder        Node search order.
         * @param currentNodeHandle Current node handle to show.
         * @return                  Image Viewer Intent.
         */
        @JvmStatic
        @JvmOverloads
        fun getIntentForTimeline(
            context: Context,
            currentNodeHandle: Long? = null,
            showSlideshow: Boolean = false,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_IS_TIMELINE, true)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
                putExtra(EXTRA_SHOW_SLIDESHOW, showSlideshow)
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
            currentNodeHandle: Long? = null,
            showSlideshow: Boolean = false,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_CHAT_ID, chatRoomId)
                putExtra(INTENT_EXTRA_KEY_MSG_ID, messageIds)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
                putExtra(EXTRA_SHOW_SLIDESHOW, showSlideshow)
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
            currentNodeHandle: Long? = null,
            showSlideshow: Boolean = false,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_ARRAY_OFFLINE, childrenHandles)
                putExtra(INTENT_EXTRA_KEY_HANDLE, currentNodeHandle)
                putExtra(EXTRA_SHOW_SLIDESHOW, showSlideshow)
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
            showNearbyFiles: Boolean = false,
            showSlideshow: Boolean = false,
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_URI, imageFileUri)
                putExtra(INTENT_EXTRA_KEY_SHOW_NEARBY_FILES, showNearbyFiles)
                putExtra(EXTRA_SHOW_SLIDESHOW, showSlideshow)
            }
    }

    @Inject
    lateinit var passCodeFacade: PasscodeCheck

    @Inject
    lateinit var getFeatureFlagUseCase: GetFeatureFlagValueUseCase

    private val nodeHandle by lazy { intent.getLongExtra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE) }
    private val nodeOfflineHandle by lazy {
        intent.getLongExtra(
            INTENT_EXTRA_KEY_OFFLINE_HANDLE,
            INVALID_HANDLE
        )
    }
    private val parentNodeHandle by lazy {
        intent.getLongExtra(
            INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
            INVALID_HANDLE
        )
    }
    private val nodeFileLink by lazy { intent.getStringExtra(EXTRA_LINK) }
    private val childrenHandles by lazy { intent.getLongArrayExtra(NODE_HANDLES) }
    private val childrenOfflineHandles by lazy {
        intent.getLongArrayExtra(
            INTENT_EXTRA_KEY_ARRAY_OFFLINE
        )
    }
    private val childOrder by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(
                INTENT_EXTRA_KEY_ORDER_GET_CHILDREN,
                SortOrder::class.java
            ) ?: SortOrder.ORDER_PHOTO_ASC
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN) as SortOrder?
                ?: SortOrder.ORDER_PHOTO_ASC
        }
    }
    private val chatRoomId by lazy { intent.getLongExtra(INTENT_EXTRA_KEY_CHAT_ID, INVALID_HANDLE) }
    private val chatMessagesId by lazy { intent.getLongArrayExtra(INTENT_EXTRA_KEY_MSG_ID) }
    private val imageFileUri by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(INTENT_EXTRA_KEY_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(INTENT_EXTRA_KEY_URI)
        }
    }
    private val showNearbyFiles by lazy {
        intent.getBooleanExtra(
            INTENT_EXTRA_KEY_SHOW_NEARBY_FILES,
            false
        )
    }
    private val showSlideshow by lazy { intent.getBooleanExtra(EXTRA_SHOW_SLIDESHOW, false) }
    private val isTimeline by lazy { intent.getBooleanExtra(EXTRA_IS_TIMELINE, false) }
    private val isFileVersion by lazy {
        intent.getBooleanExtra(
            INTENT_EXTRA_KEY_IS_FILE_VERSION,
            false
        )
    }

    private val adapterType by lazy {
        intent.getIntExtra(
            Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE,
            FROM_IMAGE_VIEWER
        )
    }

    private val viewModel by viewModels<ImageViewerViewModel>()
    private val appBarConfiguration by lazy {
        AppBarConfiguration(
            topLevelDestinationIds = setOf(),
            fallbackOnNavigateUpListener = {
                onBackPressedDispatcher.onBackPressed()
                true
            }
        )
    }

    private var bottomSheet: ImageBottomSheetDialogFragment? = null
    private var nodeSaver: NodeSaver? = null
    private var nodeAttacher: MegaAttacher? = null
    private var dragToExit: DragToExitSupport? = null
    private var dragStarted: Boolean = false

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setupAttachers(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(dragToExit?.wrapContentView(binding.root) ?: binding.root)

        setupView()
        setupNavigation()
        setupObservers(savedInstanceState == null)

        if (savedInstanceState == null) {
            if (!Fresco.hasBeenInitialized()) Fresco.initialize(this)
            binding.root.post {
                dragToExit?.runEnterAnimation(intent, binding.root) { activate ->
                    viewModel.showToolbar(!activate)
                    val color = if (activate) android.R.color.transparent else R.color.white_black
                    binding.imagesNavHostFragment.setBackgroundResource(color)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        nodeSaver?.saveState(outState)
        nodeAttacher?.saveState(outState)
    }

    override fun onLowMemory() {
        Timber.w("onLowMemory")
        viewModel.onLowMemory()
    }

    override fun onDestroy() {
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

        binding.root.post {
            // Apply system bars top and bottom insets
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.toolbar.updatePadding(0, insets.top, 0, 0)
                binding.imagesNavHostFragment.updatePadding(
                    insets.left,
                    0,
                    insets.right,
                    insets.bottom
                )
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers(requestImagesData: Boolean) {
        if (requestImagesData) {
            when {
                isTimeline ->
                    viewModel.retrieveImagesFromTimeline(nodeHandle)

                parentNodeHandle != INVALID_HANDLE ->
                    viewModel.retrieveImagesFromParent(parentNodeHandle, childOrder, nodeHandle)

                childrenHandles?.isNotEmpty() == true ->
                    viewModel.retrieveImages(childrenHandles!!, nodeHandle)

                childrenOfflineHandles?.isNotEmpty() == true ->
                    viewModel.retrieveImages(childrenOfflineHandles!!, nodeHandle, isOffline = true)

                nodeOfflineHandle != INVALID_HANDLE && nodeOfflineHandle != INVALID_HANDLE ->
                    viewModel.retrieveSingleImage(nodeOfflineHandle, isOffline = true)

                chatRoomId != INVALID_HANDLE && chatMessagesId?.isNotEmpty() == true ->
                    viewModel.retrieveChatImages(chatRoomId, chatMessagesId!!, nodeHandle)

                !nodeFileLink.isNullOrBlank() ->
                    viewModel.retrieveSingleImage(nodeFileLink!!)

                nodeHandle != INVALID_HANDLE ->
                    viewModel.retrieveSingleImage(nodeHandle)

                imageFileUri != null ->
                    viewModel.retrieveFileImage(
                        imageFileUri!!,
                        showNearbyFiles,
                        imageFileUri.hashCode().toLong()
                    )

                else ->
                    error("Invalid params")
            }
        }

        viewModel.onShowToolbar().observe(this, ::animateToolbar)
        viewModel.onSnackBarMessage().observe(this) { message ->
            bottomSheet?.dismissAllowingStateLoss()
            showSnackbar(message)
        }
        viewModel.onCopyMoveException().observe(this) { error ->
            manageCopyMoveException(error)
        }
        viewModel.onCollision().observe(this) { collision ->
            nameCollisionActivityContract?.launch(arrayListOf(collision))
        }
        viewModel.onActionBarMessage().observe(this) { message ->
            bottomSheet?.dismissAllowingStateLoss()
            showTransfersSnackBar(getString(message))
        }
    }

    private fun setupNavigation() {
        getNavController().let { navController ->
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
            navController.setGraph(
                navController.navInflater.inflate(R.navigation.nav_image_viewer).apply {
                    setStartDestination(
                        if (showSlideshow) {
                            R.id.image_slideshow
                        } else {
                            R.id.image_viewer
                        }
                    )
                },
                null
            )
        }
    }

    private fun setupAttachers(savedInstanceState: Bundle?) {
        dragToExit = DragToExitSupport(this, { activate ->
            if (activate) {
                dragStarted = true
                binding.imagesNavHostFragment.setBackgroundResource(android.R.color.transparent)
                viewModel.showToolbar(false)
            } else if (dragStarted) {
                dragStarted = false
                binding.imagesNavHostFragment.setBackgroundResource(R.color.white_black)
                viewModel.showToolbar(true)
            }
        }) {
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
     * Change toolbar visibility with animation.
     *
     * @param show  Show or hide toolbar
     */
    private fun animateToolbar(show: Boolean) {
        binding.toolbar.apply {
            post {
                val newAlpha: Float
                val newTranslationY: Float
                if (show) {
                    newAlpha = 1f
                    newTranslationY = 0f
                } else {
                    newAlpha = 0f
                    newTranslationY = -height.toFloat()
                }
                animate()
                    .alpha(newAlpha)
                    .translationY(newTranslationY)
                    .setDuration(250)
                    .start()
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val imageItem = viewModel.getCurrentImageItem() ?: return super.onPrepareOptionsMenu(menu)
        menu?.apply {
            findItem(R.id.action_slideshow)?.isVisible =
                imageItem.shouldShowSlideshowOption() && viewModel.getImagesSize(false) > 1
            findItem(R.id.action_forward)?.isVisible =
                imageItem.shouldShowForwardOption() && !isFileVersion
            findItem(R.id.action_share)?.isVisible =
                imageItem is ImageItem.ChatNode && imageItem.shouldShowShareOption() && !isFileVersion
            findItem(R.id.action_download)?.isVisible = imageItem.shouldShowDownloadOption()
            findItem(R.id.action_get_link)?.isVisible =
                imageItem.shouldShowManageLinkOption() && !isFileVersion
            findItem(R.id.action_send_to_chat)?.isVisible =
                imageItem.shouldShowSendToContactOption(viewModel.isUserLoggedIn()) && !isFileVersion
            findItem(R.id.action_more)?.isVisible = imageItem.nodeItem != null && !isFileVersion
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            R.id.action_slideshow -> {
                lifecycleScope.launch {
                    val enabled = getFeatureFlagUseCase(AppFeatures.SlideShowCompose)
                    if (enabled)
                        getNavController().navigate(ImageViewerFragmentDirections.actionViewerToNewSlideshow())
                    else
                        getNavController().navigate(ImageViewerFragmentDirections.actionViewerToSlideshow())
                }
                true
            }

            R.id.action_forward, R.id.action_send_to_chat -> {
                viewModel.getCurrentImageItem()?.nodeItem?.node?.let(::attachNode)
                true
            }

            R.id.action_share -> {
                val imageItem = viewModel.getCurrentImageItem()
                val file =
                    imageItem?.imageResult?.fullSizeUri?.toUri()?.toFile()?.takeIf { it.exists() }
                when {
                    imageItem == null ->
                        Timber.w("Image Item is null")

                    imageItem is ImageItem.OfflineNode ->
                        OfflineUtils.shareOfflineNode(this, imageItem.nodeItem!!.handle)

                    file != null -> {
                        trackOnShareClicked()
                        FileUtil.shareFile(this, file)
                    }

                    imageItem is ImageItem.PublicNode -> {
                        trackOnShareClicked()
                        MegaNodeUtil.shareLink(this, imageItem.nodePublicLink)
                    }

                    imageItem.nodeItem?.node != null -> {
                        trackOnShareClicked()
                        viewModel.exportNode(imageItem.nodeItem!!.node!!).observe(this) { link ->
                            if (!link.isNullOrBlank()) MegaNodeUtil.shareLink(this, link)
                        }
                    }

                    else ->
                        Timber.w("Node cannot be shared")
                }
                true
            }

            R.id.action_download -> {
                viewModel.getCurrentImageItem()?.nodeItem?.let { nodeItem ->
                    viewModel.executeTransfer {
                        if (nodeItem.isAvailableOffline) {
                            saveOfflineNode(nodeItem.handle)
                        } else if (nodeItem.node != null) {
                            saveNode(nodeItem.node)
                        }
                    }
                }
                true
            }

            R.id.action_get_link -> {
                viewModel.getCurrentImageItem()?.nodeItem?.handle?.let { nodeHandle ->
                    LinksUtil.showGetLinkActivity(this, nodeHandle)
                }
                true
            }

            R.id.action_more -> {
                bottomSheet = viewModel.getCurrentImageItem()?.id?.let { itemId ->
                    ImageBottomSheetDialogFragment.newInstance(itemId)
                        .apply { show(supportFragmentManager) }
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }

    fun saveNode(node: MegaNode) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver?.saveNode(
            node,
            highPriority = false,
            isFolderLink = node.isForeign,
            fromMediaViewer = true,
            needSerialize = true
        )
    }

    fun saveOfflineNode(nodeHandle: Long) {
        PermissionUtils.checkNotificationsPermission(this)
        nodeSaver?.saveOfflineNode(nodeHandle, true)
    }

    fun attachNode(node: MegaNode) {
        nodeAttacher?.attachNode(node)
    }

    fun launchVideoScreen(imageItem: ImageItem) {
        val nodeHandle = imageItem.getNodeHandle() ?: return
        val nodeName = imageItem.nodeItem?.name ?: return

        val intent = Util.getMediaIntent(this, nodeName).apply {
            putExtra(INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, adapterType)
            putExtra(
                INTENT_EXTRA_KEY_PARENT_NODE_HANDLE,
                megaApi.getNodeByHandle(nodeHandle)?.parentHandle ?: INVALID_HANDLE
            )
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val existingFile = imageItem.imageResult?.fullSizeUri?.toUri()?.toFile()
        if (existingFile?.exists() == true && existingFile.canRead()) {
            val localPath = existingFile.absolutePath ?: return
            FileUtil.setLocalIntentParams(this, nodeName, intent, localPath, false, this)
        } else {
            val node = imageItem.nodeItem?.node ?: return
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
        grantResults: IntArray,
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

    override fun onSupportNavigateUp(): Boolean =
        getNavController().navigateUp(appBarConfiguration) || super.onSupportNavigateUp()

    private fun getNavController(): NavController =
        (supportFragmentManager.findFragmentById(R.id.images_nav_host_fragment) as NavHostFragment).navController

    private fun trackOnShareClicked() {
        Analytics.tracker.trackButtonPress(LinkShareLinkTapFileButtonInfo)
    }
}
