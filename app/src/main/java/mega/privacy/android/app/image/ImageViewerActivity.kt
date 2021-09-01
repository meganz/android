package mega.privacy.android.app.image

import android.os.Bundle
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.databinding.ActivityImageViewerBinding
import mega.privacy.android.app.image.adapter.ImageViewerAdapter
import mega.privacy.android.app.utils.Constants.*
import nz.mega.documentscanner.utils.IntentUtils.extra
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

@AndroidEntryPoint
class ImageViewerActivity : BaseActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    private val viewModel by viewModels<ImageViewerViewModel>()
    private val adapter by lazy { ImageViewerAdapter(::onImageClick) }
    private val pageChangeCallback by lazy{
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val itemCount = binding.viewPager.adapter!!.itemCount
                if (!positionSet && nodePosition != null && itemCount <= nodePosition!!) {
                    positionSet = true
                    binding.viewPager.currentItem = nodePosition ?: 0
                }
            }
        }
    }

    private val nodeHandle: Long? by extra(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
    private val parentNodeHandle: Long? by extra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
    private val nodePosition: Int? by extra(INTENT_EXTRA_KEY_POSITION, INVALID_POSITION)
    private val childrenHandles: LongArray? by extra(INTENT_EXTRA_KEY_HANDLES_NODES_SEARCH)

    private var positionSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupView()
        setupObservers()

        when {
            parentNodeHandle != null && parentNodeHandle != INVALID_HANDLE ->
                viewModel.retrieveImagesFromParent(parentNodeHandle!!)
            childrenHandles != null && childrenHandles!!.isNotEmpty() ->
                viewModel.retrieveImages(childrenHandles!!.toList())
            nodeHandle != null && nodeHandle != INVALID_HANDLE ->
                viewModel.retrieveSingleImage(nodeHandle!!)
            else ->
                error("No params were sent")
        }
    }

    override fun onDestroy() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroy()
    }

    private fun setupView() {
        binding.viewPager.adapter = adapter
        binding.viewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    private fun setupObservers() {
        viewModel.getImages().observe(this) { images ->
            adapter.submitList(images)
        }
    }

    private fun onImageClick(nodeHandle: Long) {
        //do something
    }
}
