package mega.privacy.android.app.imageviewer

import android.annotation.SuppressLint
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
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.imageviewer.dialog.ImageBottomSheetDialogFragment
import mega.privacy.android.app.interfaces.PermissionRequester
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.AlertsAndWarnings.showSaveToDeviceConfirmDialog
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.documentscanner.utils.IntentUtils.extraNotNull
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC
import nz.mega.sdk.MegaNode
import java.lang.ref.WeakReference

@AndroidEntryPoint
class ImageViewerActivity : BaseActivity(), PermissionRequester, SnackbarShower {

    companion object {
        private const val OFFSCREEN_PAGE_LIMIT = 3
    }

    private val nodePosition: Int by extraNotNull(INTENT_EXTRA_KEY_POSITION, 0)
    private val nodeHandle: Long? by extra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
    private val parentNodeHandle: Long? by extra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
    private val childrenHandles: LongArray? by extra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)
    private val childOrder: Int by extraNotNull(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_PHOTO_ASC)

    private var defaultPageSet = false
    private val viewModel by viewModels<ImageViewerViewModel>()
    private val pagerAdapter by lazy { ImageViewerAdapter(this) }
    private val pageChangeCallback by lazy {
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val itemCount = binding.viewPager.adapter?.itemCount ?: 0

                binding.txtPageCount.apply {
                    text = getString(
                        R.string.wizard_steps_indicator,
                        position + 1,
                        itemCount
                    )
                    isVisible = itemCount > 1
                }

                viewModel.setCurrentPosition(position)
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

        setupView()

        when {
            parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE -> {
                viewModel.retrieveImagesFromParent(parentNodeHandle!!, childOrder)
            }
            childrenHandles != null && childrenHandles!!.isNotEmpty() -> {
                viewModel.retrieveImages(childrenHandles!!.toList())
            }
            nodeHandle != null && nodeHandle != INVALID_HANDLE -> {
                viewModel.retrieveSingleImage(nodeHandle!!)
            }
            else -> {
                error("Invalid params")
            }
        }

        setupObservers()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_image_viewer, menu)
        return true
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    @SuppressLint("WrongConstant")
    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.viewPager.apply {
            adapter = pagerAdapter
            offscreenPageLimit = OFFSCREEN_PAGE_LIMIT
            registerOnPageChangeCallback(pageChangeCallback)
        }
    }

    private fun setupObservers() {
        viewModel.setCurrentPosition(nodePosition)
        viewModel.getImagesHandle().observe(this) { handles ->
            pagerAdapter.submitList(handles) {
                if (!defaultPageSet) {
                    defaultPageSet = true
                    binding.viewPager.waitForLayout {
                        binding.viewPager.setCurrentItem(nodePosition, false)
                    }
                }
            }
        }
        viewModel.getCurrentImage().observe(this, ::showCurrentImage)
    }

    private fun showCurrentImage(item: ImageItem?) {
        if (item != null) {
            binding.txtTitle.text = item.name
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentNodeHandle = viewModel.getCurrentHandle()

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
}
