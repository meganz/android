package mega.privacy.android.app.imageviewer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.components.attacher.MegaAttacher
import mega.privacy.android.app.components.saver.NodeSaver
import mega.privacy.android.app.databinding.ActivityImageViewerBinding
import mega.privacy.android.app.imageviewer.adapter.ImageViewerAdapter
import mega.privacy.android.app.imageviewer.dialog.ImageBottomSheetDialogFragment
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.usecase.data.MegaNodeItem
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ARRAY_OFFLINE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ORDER_GET_CHILDREN
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_NODE_HANDLE
import mega.privacy.android.app.utils.Constants.NODE_HANDLES
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.NetworkUtil.isOnline
import mega.privacy.android.app.utils.ViewUtils.setStatusBarTransparent
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference

@AndroidEntryPoint
class ImageViewerActivity : BaseActivity(), PermissionRequester, SnackbarShower, ActionNodeCallback {

    companion object {
        private const val OFFSCREEN_PAGE_LIMIT = 3
        private const val KEY_DEFAULT_PAGE_SET = "KEY_DEFAULT_PAGE_SET"

        @JvmStatic
        fun getIntentForSingleNode(
            context: Context,
            nodeHandle: Long
        ): Intent =
            Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
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
    private val childrenHandles: LongArray? by extra(NODE_HANDLES)
    private val childrenOfflineHandles: LongArray? by extra(INTENT_EXTRA_KEY_ARRAY_OFFLINE)
    private val childOrder: Int? by extra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN)

    private var defaultPageSet = false
    private val viewModel by viewModels<ImageViewerViewModel>()
    private val pagerAdapter by lazy { ImageViewerAdapter(this) }
    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                viewModel.updateCurrentPosition(position)
            }
        }
    }
    private val nodeAttacher by lazy { WeakReference(MegaAttacher(this)) }
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
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState?.containsKey(KEY_DEFAULT_PAGE_SET) == true) {
            defaultPageSet = savedInstanceState.getBoolean(KEY_DEFAULT_PAGE_SET)
        }

        setupView()
        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image_viewer, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_DEFAULT_PAGE_SET, defaultPageSet)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    @SuppressLint("WrongConstant")
    private fun setupView() {
        setStatusBarTransparent()
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
            registerOnPageChangeCallback(pageChangeCallback)
        }
    }

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
            else ->
                error("Invalid params")
        }

        viewModel.getImagesHandle().observe(this, pagerAdapter::submitList)
        viewModel.getCurrentImage().observe(this, ::showCurrentImageInfo)
        viewModel.onSwitchToolbar().observe(this) { switchToolbarVisibility() }
        viewModel.getInitialPosition().observe(this) { position ->
            if (!defaultPageSet) {
                defaultPageSet = true
                binding.viewPager.waitForLayout {
                    binding.viewPager.setCurrentItem(position, false)
                }
            }
        }
    }

    private fun showCurrentImageInfo(item: MegaNodeItem?) {
        item?.let {
            binding.txtTitle.text = item.node.name

            val itemCount = binding.viewPager.adapter?.itemCount ?: 0
            if (itemCount > 1) {
                val currentItem = binding.viewPager.currentItem + 1
                binding.txtPageCount.text = getString(R.string.wizard_steps_indicator, currentItem, itemCount)
                binding.txtPageCount.isVisible = true
            } else {
                binding.txtPageCount.isVisible = false
            }

            binding.toolbar.menu?.apply {
                if (isOnline() && !item.isFromRubbishBin) {
                    findItem(R.id.action_download)?.isVisible = true
                    findItem(R.id.action_save_gallery)?.isVisible = true
                    findItem(R.id.action_get_link)?.isVisible = item.hasFullAccess
                    findItem(R.id.action_chat)?.isVisible = true
                } else {
                    findItem(R.id.action_download)?.isVisible = false
                    findItem(R.id.action_save_gallery)?.isVisible = false
                    findItem(R.id.action_get_link)?.isVisible = false
                    findItem(R.id.action_chat)?.isVisible = false
                }
            }
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
        val currentNodeHandle = viewModel.getCurrentHandle().value!!

        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_download -> {
                saveNode(currentNodeHandle, false)
                true
            }
            R.id.action_save_gallery -> {
                saveNode(currentNodeHandle, true)
                true
            }
            R.id.action_get_link -> {
                LinksUtil.showGetLinkActivity(this, currentNodeHandle)
                true
            }
            R.id.action_chat -> {
                attachNode(currentNodeHandle)
                true
            }
            R.id.action_more -> {
                ImageBottomSheetDialogFragment.newInstance(currentNodeHandle)
                    .show(supportFragmentManager)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun saveNode(nodeHandle: Long, downloadToGallery: Boolean) {
        nodeSaver.get()?.saveHandle(
            nodeHandle,
            highPriority = false,
            isFolderLink = false,
            fromMediaViewer = true,
            needSerialize = true,
            downloadToGallery = downloadToGallery
        )
    }

    fun attachNode(nodeHandle: Long) {
        nodeAttacher.get()?.attachNode(nodeHandle)
    }

    fun showRenameDialog(node: MegaNode) {
        showRenameNodeDialog(this, node, this, this)
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

    override fun actionConfirmed() {
        viewModel.reloadCurrentImage(false)
    }

    override fun showSnackbar(type: Int, content: String?, chatId: Long) {
        showSnackbar(type, binding.root, content, chatId)
    }
}
