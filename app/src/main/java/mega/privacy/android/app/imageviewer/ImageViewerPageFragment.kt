package mega.privacy.android.app.imageviewer

import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils.ScaleType
import com.facebook.imagepipeline.common.Priority
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.memory.BasePool
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.PageImageViewerBinding
import mega.privacy.android.app.imageviewer.data.ImageResult
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.ContextUtils.getScreenSize
import mega.privacy.android.app.utils.ExtraUtils.extra
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.view.MultiTapGestureListener

/**
 * Image Viewer page that shows an individual image within a list of image items
 */
@AndroidEntryPoint
class ImageViewerPageFragment : Fragment() {

    companion object {
        private const val IMAGE_SCALE_FACTOR = 4f

        /**
         * Main method to create a ImageViewerPageFragment.
         *
         * @param itemId        Item to show
         * @return              ImageBottomSheetDialogFragment to be shown
         */
        fun newInstance(itemId: Long): ImageViewerPageFragment =
            ImageViewerPageFragment().apply {
                arguments = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, itemId)
                }
            }
    }

    private lateinit var binding: PageImageViewerBinding

    private var hasScreenBeenRotated = false
    private var hasZoomBeenTriggered = false
    private val viewModel by activityViewModels<ImageViewerViewModel>()
    private val itemId: Long? by extra(INTENT_EXTRA_KEY_HANDLE)
    private val controllerListener by lazy { buildImageControllerListener() }
    private val screenSize: Size by lazy { requireContext().getScreenSize() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireNotNull(itemId)
        hasScreenBeenRotated = savedInstanceState != null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PageImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        if (!hasScreenBeenRotated) {
            showFullImage()
        }
    }

    override fun onPause() {
        if (activity?.isChangingConfigurations != true && activity?.isFinishing != true) {
            viewModel.stopImageLoading(itemId!!, aggressive = false)
            showPreviewImage()
        }
        super.onPause()
    }

    override fun onDestroy() {
        if (activity?.isFinishing == true) {
            viewModel.stopImageLoading(itemId!!, aggressive = true)
        }
        super.onDestroy()
    }

    private fun setupView() {
        binding.image.apply {
            setZoomingEnabled(true)
            setIsLongpressEnabled(true)
            setAllowTouchInterceptionWhileZoomed(false)
            setMaxScaleFactor(IMAGE_SCALE_FACTOR)
            setTapListener(
                MultiTapGestureListener(
                    this,
                    onSingleTapCallback = viewModel::switchToolbar,
                    onZoomCallback = {
                        if (!hasZoomBeenTriggered) {
                            hasZoomBeenTriggered = true
                            viewModel.loadSingleImage(itemId!!, fullSize = true)
                        }
                    }
                )
            )
        }
    }

    private fun setupObservers() {
        viewModel.onImage(itemId!!).observe(viewLifecycleOwner) { imageItem ->
            val imageResult = imageItem?.imageResult ?: return@observe

            when (lifecycle.currentState) {
                Lifecycle.State.RESUMED ->
                    showFullImage(imageResult)
                Lifecycle.State.CREATED, Lifecycle.State.STARTED ->
                    showPreviewImage(imageResult)
                else -> {
                    // do nothing
                }
            }
        }

        if (!hasScreenBeenRotated) {
            viewModel.loadSingleNode(itemId!!)
            viewModel.loadSingleImage(itemId!!, fullSize = false)
        }
    }

    /**
     * Show thumbnail and preview images
     *
     * @param imageResult   ImageResult to obtain images from
     */
    private fun showPreviewImage(
        imageResult: ImageResult? = viewModel.getImageItem(itemId!!)?.imageResult
    ) {
        val previewImageRequest = imageResult?.previewUri?.toImageRequest()
        val thumbnailImageRequest = imageResult?.thumbnailUri?.toImageRequest()
        if (previewImageRequest == null && thumbnailImageRequest == null) return

        val newControllerBuilder = Fresco.newDraweeControllerBuilder()
        if (previewImageRequest != null) {
            newControllerBuilder.imageRequest = previewImageRequest
            thumbnailImageRequest?.let { newControllerBuilder.setLowResImageRequest(it) }
        } else {
            newControllerBuilder.imageRequest = thumbnailImageRequest
        }

        if (binding.image.controller?.isSameImageRequest(newControllerBuilder.build()) != true) {
            binding.image.controller = newControllerBuilder
                .setOldController(binding.image.controller)
                .setControllerListener(controllerListener)
                .setAutoPlayAnimations(true)
                .build()

            if (imageResult.isVideo) {
                binding.image.post { showVideoButton() }
            }
        }
    }

    /**
     * Show full image with preview as placeholder
     *
     * ImageResult to obtain images from
     */
    private fun showFullImage(
        imageResult: ImageResult? = viewModel.getImageItem(itemId!!)?.imageResult
    ) {
        val fullImageRequest = imageResult?.fullSizeUri?.toImageRequest() ?: run {
            showPreviewImage(imageResult)
            return
        }
        val previewImageRequest = (imageResult.previewUri ?: imageResult.thumbnailUri)?.toImageRequest()

        val newControllerBuilder = Fresco.newDraweeControllerBuilder()
            .setImageRequest(fullImageRequest)
        if (previewImageRequest != null) {
            newControllerBuilder.setLowResImageRequest(previewImageRequest)
        }

        if (binding.image.controller?.isSameImageRequest(newControllerBuilder.build()) != true) {
            binding.image.controller = newControllerBuilder
                .setOldController(binding.image.controller)
                .setControllerListener(controllerListener)
                .setAutoPlayAnimations(true)
                .build()

            if (imageResult.isVideo) {
                binding.image.post { showVideoButton() }
            }
        }
        binding.progress.hide()
    }

    private fun buildImageControllerListener() = object : BaseControllerListener<ImageInfo>() {
        override fun onFinalImageSet(
            id: String?,
            imageInfo: ImageInfo?,
            animatable: Animatable?
        ) {
            val imageResult = viewModel.getImageItem(itemId!!)?.imageResult ?: return
            if (imageResult.isFullyLoaded) {
                binding.image.post {
                    if (imageResult.isVideo) showVideoButton()
                }
            }
        }

        override fun onFailure(id: String, throwable: Throwable) {
            logError(throwable.stackTraceToString())
            if (throwable is BasePool.PoolSizeViolationException) activity?.onLowMemory()

            val imageResult = viewModel.getImageItem(itemId!!)?.imageResult ?: return
            binding.image.hierarchy.setFailureImage(R.drawable.ic_error, ScaleType.FIT_CENTER)
            binding.image.controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageResult.previewUri?.toImageRequest())
                .build()

            if (imageResult.isFullyLoaded) {
                binding.image.post {
                    if (imageResult.isVideo) showVideoButton()
                }
            }
        }
    }

    private fun showVideoButton() {
        if (binding.btnVideo.isVisible && viewModel.isToolbarShown()) return

        viewModel.switchToolbar(true)
        binding.btnVideo.setOnClickListener { launchVideoScreen() }
        binding.btnVideo.isVisible = true
        binding.image.apply {
            setAllowTouchInterceptionWhileZoomed(true)
            setZoomingEnabled(false)
            setTapListener(object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent?): Boolean {
                    launchVideoScreen()
                    return true
                }
            })
            setOnClickListener { launchVideoScreen() }
        }
    }

    private fun launchVideoScreen() {
        val imageItem = viewModel.getImageItem(itemId!!) ?: return
        (activity as? ImageViewerActivity?)?.launchVideoScreen(imageItem)
    }

    private fun Uri.toImageRequest(): ImageRequest? =
        ImageRequestBuilder.newBuilderWithSource(this)
            .setRotationOptions(RotationOptions.autoRotate())
            .setResizeOptions(ResizeOptions.forDimensions(screenSize.width, screenSize.height))
            .setRequestPriority(
                if (lifecycle.currentState == Lifecycle.State.RESUMED)
                    Priority.HIGH
                else
                    Priority.LOW
            )
            .build()
}
