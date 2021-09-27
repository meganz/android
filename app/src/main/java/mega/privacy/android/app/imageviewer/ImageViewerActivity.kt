package mega.privacy.android.app.imageviewer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.ActivityImageViewerBinding
import mega.privacy.android.app.imageviewer.adapter.ImageViewerAdapter
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LinksUtil
import mega.privacy.android.app.utils.ViewUtils.waitForLayout
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.documentscanner.utils.IntentUtils.extraNotNull
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_PHOTO_ASC

@AndroidEntryPoint
class ImageViewerActivity : BaseActivity() {

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

                viewModel.setPosition(position)
            }
        }
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
        viewModel.setPosition(nodePosition)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_download -> {
                //do something
                true
            }
            R.id.action_save_gallery -> {
                //do something
                true
            }
            R.id.action_get_link -> {
                viewModel.getCurrentImage().value?.handle?.let { currentHandle ->
                    LinksUtil.showGetLinkActivity(this, currentHandle)
                }
                true
            }
            R.id.action_chat -> {
                //do something
                true
            }
            R.id.action_more -> {
                //do something
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
}
