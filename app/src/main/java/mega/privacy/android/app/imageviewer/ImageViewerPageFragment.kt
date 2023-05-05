package mega.privacy.android.app.imageviewer

import android.graphics.Matrix
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.util.Size
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
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
import com.facebook.samples.zoomable.AbstractAnimatedZoomableController
import com.google.firebase.perf.FirebasePerformance
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.PageImageViewerBinding
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.ContextUtils.getScreenSize
import mega.privacy.android.app.utils.ContextUtils.isLowMemory
import mega.privacy.android.app.utils.PerformanceRequestListener
import mega.privacy.android.app.utils.view.MultiTapGestureListener
import mega.privacy.android.domain.entity.imageviewer.ImageResult
import timber.log.Timber
import javax.inject.Inject

/**
 * Image Viewer page that shows an individual image within a list of image items
 */
@AndroidEntryPoint
class ImageViewerPageFragment : Fragment() {

    companion object {
        private const val EXTRA_ENABLE_ZOOM = "EXTRA_ENABLE_ZOOM"
        private const val MAX_ZOOM_SCALE_FACTOR = 20f
        private const val MAX_BITMAP_SIZE = 5000 // Android Canvas maximum bitmap size
        private const val PERF_TRACE_NAME = "full_image_loading"

        /**
         * Main method to create a ImageViewerPageFragment.
         *
         * @param itemId        Item to show
         * @param enableZoom    Flag to enable zoom gestures
         * @return              ImageBottomSheetDialogFragment to be shown
         */
        fun newInstance(itemId: Long, enableZoom: Boolean): ImageViewerPageFragment =
            ImageViewerPageFragment().apply {
                arguments = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, itemId)
                    putBoolean(EXTRA_ENABLE_ZOOM, enableZoom)
                }
            }
    }

    @Inject
    lateinit var firebasePerf: FirebasePerformance

    private lateinit var binding: PageImageViewerBinding

    private var hasScreenBeenRotated = false
    private var hasZoomBeenTriggered = false
    private val viewModel by activityViewModels<ImageViewerViewModel>()
    private val itemId by lazy {
        arguments?.getLong(INTENT_EXTRA_KEY_HANDLE) ?: error("Null Item Id")
    }
    private val enableZoom by lazy { arguments?.getBoolean(EXTRA_ENABLE_ZOOM, true) ?: true }
    private val controllerListener by lazy { buildImageControllerListener() }
    private val screenSize: Size by lazy { requireContext().getScreenSize() }
    private var transform: Matrix = Matrix()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hasScreenBeenRotated = savedInstanceState != null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = PageImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
        viewModel.startSlideshow()
    }

    override fun onResume() {
        super.onResume()
        if (!hasScreenBeenRotated) {
            showFullImage()
        }
    }

    override fun onPause() {
        if (activity?.isChangingConfigurations != true && activity?.isFinishing != true) {
            showPreviewImage()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        if (activity?.isFinishing == true) {
            viewModel.stopImageLoading(itemId)
        }
        super.onDestroyView()
    }

    private fun setupView() {
        binding.image.apply {
            setZoomingEnabled(enableZoom)
            setAllowTouchInterceptionWhileZoomed(!enableZoom)
            setIsLongpressEnabled(enableZoom)
            setMaxScaleFactor(MAX_ZOOM_SCALE_FACTOR)
            if (enableZoom) {
                setTapListener(
                    MultiTapGestureListener(
                        this,
                        onSingleTapCallback = {
                            viewModel.showToolbar(!viewModel.isToolbarShown())
                        },
                        onTransformChanged = { changedTransform ->
                            if (!hasZoomBeenTriggered) {
                                hasZoomBeenTriggered = true
                                viewModel.loadSingleImage(itemId, fullSize = true)
                            }
                            transform = Matrix(changedTransform)
                        }
                    )
                )
            } else {
                setTapListener(object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        viewModel.showToolbar(!viewModel.isToolbarShown())
                        return true
                    }

                    override fun onScroll(
                        e1: MotionEvent,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float,
                    ): Boolean {
                        if (e2.pointerCount > 1) navigateToViewer()
                        return super.onScroll(e1, e2, distanceX, distanceY)
                    }
                })
                setOnClickListener {
                    viewModel.showToolbar(!viewModel.isToolbarShown())
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.onImage(itemId).observe(viewLifecycleOwner) { imageItem ->
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

            imageResult.getProgressPercentage()?.let {
                binding.progress.progress = it
            }
        }

        if (!hasScreenBeenRotated) {
            viewModel.loadSingleNode(itemId)
            viewModel.loadSingleImage(itemId, fullSize = false)
        }
    }

    /**
     * Show thumbnail and preview images
     *
     * @param imageResult   ImageResult to obtain images from
     */
    private fun showPreviewImage(
        imageResult: ImageResult? = viewModel.getImageItem(itemId)?.imageResult,
    ) {
        val previewImageRequest = imageResult?.previewUri?.toUri()?.toImageRequest(false)
        val thumbnailImageRequest = imageResult?.thumbnailUri?.toUri()?.toImageRequest(false)
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
        imageResult: ImageResult? = viewModel.getImageItem(itemId)?.imageResult,
    ) {
        val fullImageRequest = imageResult?.fullSizeUri?.toUri()?.toImageRequest(true) ?: run {
            showPreviewImage(imageResult)
            return
        }
        val previewImageRequest =
            (imageResult.previewUri ?: imageResult.thumbnailUri)?.toUri()?.toImageRequest(false)

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
            attachTransform()

            if (imageResult.isVideo) {
                binding.image.post { showVideoButton() }
            }
        }
    }

    //Attach ZoomableDraweeView's transform to latest value in order to Retain Current Offset and Zoom
    private fun attachTransform() {
        val zoomableController =
            binding.image.zoomableController as AbstractAnimatedZoomableController
        zoomableController.transform = transform
    }

    private fun buildImageControllerListener() = object : BaseControllerListener<ImageInfo>() {
        override fun onFinalImageSet(
            id: String?,
            imageInfo: ImageInfo?,
            animatable: Animatable?,
        ) {
            val imageResult = viewModel.getImageItem(itemId)?.imageResult ?: return
            if (imageResult.isFullyLoaded) {
                binding.image.post {
                    if (imageResult.isVideo) showVideoButton()
                    binding.progress.hide()
                }
            }
        }

        override fun onFailure(id: String, throwable: Throwable) {
            Timber.e(throwable)
            if (throwable is BasePool.PoolSizeViolationException) activity?.onLowMemory()

            val imageResult = viewModel.getImageItem(itemId)?.imageResult ?: return
            binding.image.hierarchy.setFailureImage(R.drawable.ic_error, ScaleType.FIT_CENTER)
            binding.image.controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageResult.previewUri?.toUri()?.toImageRequest(false))
                .build()
            attachTransform()

            if (imageResult.isFullyLoaded) {
                binding.image.post {
                    if (imageResult.isVideo) showVideoButton()
                    binding.progress.hide()
                }
            }
        }
    }

    private fun showVideoButton() {
        if (binding.btnVideo.isVisible && viewModel.isToolbarShown()) return

        viewModel.showToolbar(true)
        binding.btnVideo.setOnClickListener { launchVideoScreen() }
        binding.btnVideo.isVisible = true
        binding.image.apply {
            setAllowTouchInterceptionWhileZoomed(true)
            setZoomingEnabled(false)
            setTapListener(object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    launchVideoScreen()
                    return true
                }
            })
            setOnClickListener { launchVideoScreen() }
        }
    }

    private fun launchVideoScreen() {
        val imageItem = viewModel.getImageItem(itemId) ?: return
        (activity as? ImageViewerActivity?)?.launchVideoScreen(imageItem)
    }

    private fun navigateToViewer() {
        if (!findNavController().popBackStack()) {
            findNavController().navigate(R.id.image_viewer)
        }
    }

    private fun Uri.toImageRequest(isFullImage: Boolean): ImageRequest? =
        ImageRequestBuilder.newBuilderWithSource(this)
            .setRotationOptions(RotationOptions.autoRotate())
            .setRequestListener(
                if (isFullImage) {
                    PerformanceRequestListener(firebasePerf, PERF_TRACE_NAME)
                } else {
                    null
                }
            )
            .setRequestPriority(
                if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                    Priority.HIGH
                } else {
                    Priority.LOW
                }
            )
            .setResizeOptions(
                if (isFullImage && !requireContext().isLowMemory()) {
                    ResizeOptions(MAX_BITMAP_SIZE, MAX_BITMAP_SIZE, MAX_BITMAP_SIZE.toFloat())
                } else {
                    ResizeOptions.forDimensions(screenSize.width, screenSize.height)
                }
            )
            .setCacheChoice(
                if (isFullImage) {
                    ImageRequest.CacheChoice.DEFAULT
                } else {
                    ImageRequest.CacheChoice.SMALL
                }
            )
            .build()
}
