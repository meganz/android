package mega.privacy.android.app.imageviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.*
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityImageViewerBinding
import mega.privacy.android.app.imageviewer.adapter.ImageViewerAdapter
import mega.privacy.android.app.imageviewer.dialog.ImageBottomSheetDialogFragment
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.LogUtil.logWarning
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.NetworkUtil.isOnline
import mega.privacy.android.app.utils.StringResourcesUtils.getQuantityString
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference


@AndroidEntryPoint
class ImageViewerActivity : BaseActivity(), PermissionRequester, SnackbarShower {

    companion object {
        private const val OFFSCREEN_PAGE_LIMIT = 3
        private const val STATE_PAGE_CALLBACK_SET = "STATE_PAGE_CALLBACK_SET"

        @JvmStatic
        fun getIntentForSingleNode(
            context: Context,
            nodeHandle: Long
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
            }

        @JvmStatic
        fun getIntentForSingleNode(
            context: Context,
            nodeFileLink: String
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(EXTRA_LINK, nodeFileLink)
            }

        @JvmStatic
        fun getIntentForSingleOfflineNode(
            context: Context,
            nodeHandle: Long
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_OFFLINE_HANDLE, nodeHandle)
            }

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
    }

    private val nodeHandle: Long? by extra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
    private val nodeOfflineHandle: Long? by extra(INTENT_EXTRA_KEY_OFFLINE_HANDLE, INVALID_HANDLE)
    private val parentNodeHandle: Long? by extra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
    private val nodeFileLink: String? by extra(EXTRA_LINK)
    private val childrenHandles: LongArray? by extra(NODE_HANDLES)
    private val childrenOfflineHandles: LongArray? by extra(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
    private val childOrder: Int? by extra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN)

    private var pageCallbackSet = false
    private val viewModel by viewModels<ImageViewerViewModel>()
    private val pagerAdapter by lazy { ImageViewerAdapter(this) }
    private val nodeAttacher by lazy { WeakReference(MegaAttacher(this)) }
    private var bottomSheet: ImageBottomSheetDialogFragment? = null
    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateCurrentPosition(position)
            }
        }
    }
    private val nodeSaver by lazy {
        WeakReference(
            NodeSaver(
                this, this, this,
                showSaveToDeviceConfirmDialog(this)
            )
        )
    }

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pageCallbackSet = savedInstanceState?.getBoolean(STATE_PAGE_CALLBACK_SET) ?: pageCallbackSet
        setupView()
        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image_viewer, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_PAGE_CALLBACK_SET, pageCallbackSet)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        nodeAttacher.clear()
        nodeSaver.clear()
        super.onDestroy()
    }

    @SuppressLint("WrongConstant")
    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
        }

        // Apply statusBar top inset as padding for toolbar
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val topInset = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(0, topInset, 0, 0)
            WindowInsetsCompat.CONSUMED
        }

        // Apply navigationBar bottom inset as margin for txtPageCount
        ViewCompat.setOnApplyWindowInsetsListener(binding.txtPageCount) { view, windowInsets ->
            val bottomInset = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = bottomInset }

            binding.motion.apply { // Needs to also update margins on MotionsLayout's Scene
                constraintSetIds.forEach { id ->
                    getConstraintSet(id).apply {
                        setMargin(view.id, ConstraintSet.BOTTOM, bottomInset)
                    }
                }
            }
            WindowInsetsCompat.CONSUMED
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupObservers() {
        when {
            parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE ->
                viewModel.retrieveImagesFromParent(parentNodeHandle!!, childOrder, nodeHandle)
            childrenHandles != null && childrenHandles!!.isNotEmpty() ->
                viewModel.retrieveImages(childrenHandles!!, nodeHandle)
            childrenOfflineHandles != null && childrenOfflineHandles!!.isNotEmpty() ->
                viewModel.retrieveOfflineImages(childrenOfflineHandles!!, nodeHandle)
            nodeOfflineHandle != null && nodeOfflineHandle != INVALID_HANDLE ->
                viewModel.retrieveSingleOfflineImage(nodeOfflineHandle!!)
            nodeHandle != null && nodeHandle != INVALID_HANDLE ->
                viewModel.retrieveSingleImage(nodeHandle!!)
            !nodeFileLink.isNullOrBlank() ->
                viewModel.retrieveSingleImage(nodeFileLink!!)
            else ->
                error("Invalid params")
        }

        viewModel.getImagesHandle().observe(this) { items ->
            if (items.isNullOrEmpty()) {
                logError("Null image items")
                finish()
            } else {
                val sizeDifference = pagerAdapter.itemCount != items.size
                pagerAdapter.submitList(items) {
                    if (sizeDifference) {
                        pagerAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
        viewModel.getCurrentImage().observe(this, ::showCurrentImageInfo)
        viewModel.onSwitchToolbar().observe(this) { switchToolbarVisibility() }
        viewModel.getCurrentPosition().observe(this) { positionPair ->
            binding.txtPageCount.apply {
                text = getString(
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

    private fun showCurrentImageInfo(item: MegaNodeItem?) {
        if (item != null) {
            bottomSheet?.dismiss()

            binding.txtTitle.text = item.node.name
            binding.toolbar.menu?.apply {
                findItem(R.id.action_download)?.isVisible =
                    isOnline() && item.hasFullAccess && !item.isFromRubbishBin

                findItem(R.id.action_save_gallery)?.isVisible =
                    isOnline() && !item.isFromRubbishBin

                findItem(R.id.action_get_link)?.isVisible =
                    isOnline() && item.hasFullAccess && !item.isFromRubbishBin

                findItem(R.id.action_chat)?.isVisible =
                    isOnline() && item.hasFullAccess && !item.isFromRubbishBin
            }
        } else {
            logWarning("Null image item")
        }
    }

    private fun switchToolbarVisibility() {
        if (binding.motion.currentState == R.id.start) {
            binding.motion.transitionToEnd()
        } else {
            binding.motion.transitionToStart()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentNode = viewModel.getCurrentNode() ?: return true

        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_download -> {
                saveNode(currentNode, false)
                true
            }
            R.id.action_save_gallery -> {
                saveNode(currentNode, true)
                true
            }
            R.id.action_get_link -> {
                LinksUtil.showGetLinkActivity(this, currentNode.handle)
                true
            }
            R.id.action_chat -> {
                attachNode(currentNode)
                true
            }
            R.id.action_more -> {
                bottomSheet = ImageBottomSheetDialogFragment.newInstance(currentNode.handle).apply {
                    show(supportFragmentManager)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun saveNode(node: MegaNode, downloadToGallery: Boolean) {
        nodeSaver.get()?.saveNode(
            node,
            highPriority = false,
            isFolderLink = false,
            fromMediaViewer = true,
            needSerialize = true,
            downloadToGallery = downloadToGallery
        )
    }

    fun attachNode(node: MegaNode) {
        nodeAttacher.get()?.attachNode(node)
    }

    fun removeLink(nodeHandle: Long) {
        viewModel.removeLink(nodeHandle).observe(this) { isSuccess ->
            if (isSuccess) {
                showSnackbar(getQuantityString(R.plurals.context_link_removal_success, 1))
            } else {
                showSnackbar(getQuantityString(R.plurals.context_link_removal_error, 1))
            }
        }
    }

    fun showRenameDialog(node: MegaNode) {
        showRenameNodeDialog(this, node, this, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        when {
            nodeAttacher.get()?.handleActivityResult(requestCode, resultCode, intent, this) == true ->
                return
            nodeSaver.get()?.handleActivityResult(requestCode, resultCode, intent) == true ->
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
