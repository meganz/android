package mega.privacy.android.app.imageviewer

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.databinding.PageImageViewerBinding
import mega.privacy.android.app.imageviewer.data.ImageItem
import mega.privacy.android.app.mediaplayer.VideoPlayerActivity
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.NetworkUtil.isMeteredConnection
import mega.privacy.android.app.utils.view.MultiTapGestureListener
import nz.mega.documentscanner.utils.IntentUtils.extraNotNull
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import javax.inject.Inject

/**
 * Image Viewer page that shows an individual image within a list of image items
 */
@AndroidEntryPoint
class ImageViewerPageFragment : Fragment() {

    companion object {
        private const val STATE_FULL_IMAGE_REQUESTED = "STATE_FULL_IMAGE_REQUESTED"

        fun newInstance(nodeHandle: Long): ImageViewerPageFragment =
            ImageViewerPageFragment().apply {
                arguments = Bundle().apply {
                    putLong(INTENT_EXTRA_KEY_HANDLE, nodeHandle)
                }
            }
    }

    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var binding: PageImageViewerBinding

    private val viewModel by activityViewModels<ImageViewerViewModel>()
    private var fullImageRequested = false
    private val nodeHandle: Long by extraNotNull(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PageImageViewerBinding.inflate(inflater, container, false)
        fullImageRequested = savedInstanceState?.getBoolean(STATE_FULL_IMAGE_REQUESTED) ?: fullImageRequested
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        require(nodeHandle != INVALID_HANDLE) { "Invalid node handle" }
        setupView()
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        if (!isHighResolutionRestricted()) {
            requestFullSizeImage()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_FULL_IMAGE_REQUESTED, fullImageRequested)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        viewModel.stopImageLoading(nodeHandle)
        super.onDestroyView()
    }

    private fun setupView() {
        binding.image.apply {
            setZoomingEnabled(true)
            setIsLongpressEnabled(true)
            setAllowTouchInterceptionWhileZoomed(false)
            setTapListener(
                MultiTapGestureListener(
                    this,
                    onSingleTapCallback = viewModel::switchToolbar,
                    onZoomCallback = ::requestFullSizeImage
                )
            )
        }
    }

    private fun setupObservers() {
        viewModel.onImage(nodeHandle).observe(viewLifecycleOwner, ::showItem)
        viewModel.loadSingleNode(nodeHandle)
        viewModel.loadSingleImage(nodeHandle, fullSize = false, highPriority = false)
    }

    private fun showItem(item: ImageItem?) {
        if (item?.imageResult != null) {
            val imageResult = item.imageResult
            when {
                imageResult.fullSizeUri != null && imageResult.isVideo ->
                    showImageUris(imageResult.fullSizeUri!!, null)
                imageResult.fullSizeUri != null ->
                    showImageUris(imageResult.fullSizeUri!!, imageResult.previewUri ?: imageResult.thumbnailUri)
                imageResult.previewUri != null && !imageResult.isVideo ->
                    showImageUris(imageResult.previewUri!!, imageResult.thumbnailUri)
                imageResult.thumbnailUri != null && !imageResult.isVideo ->
                    showImageUris(imageResult.thumbnailUri!!)
            }

            if (imageResult.fullyLoaded) {
                if (imageResult.isVideo) {
                    binding.btnVideo.setOnClickListener { launchVideoScreen(item) }
                    binding.image.apply {
                        setZoomingEnabled(false)
                        setIsLongpressEnabled(false)
                        setTapListener(object : GestureDetector.SimpleOnGestureListener() {
                            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                                launchVideoScreen(item)
                                return true
                            }
                        })
                    }
                }
                binding.btnVideo.isVisible = imageResult.isVideo
                binding.progress.hide()
            }
        }
    }

    /**
     * Show a pair of Image Uris on the current DraweeView.
     * Using a lower res image to preload the next one.
     *
     * @param mainImageUri      Higher resolution Image uri to show
     * @param lowResImageUri    Lower resolution Image uri to show
     */
    private fun showImageUris(mainImageUri: Uri, lowResImageUri: Uri? = null) {
        val controller = Fresco.newDraweeControllerBuilder()
            .setAutoPlayAnimations(true)
            .setControllerListener(object : BaseControllerListener<ImageInfo>() {
                override fun onFailure(id: String, throwable: Throwable) {
                    logError(throwable.stackTraceToString())
                }
            })
            .setLowResImageRequest(ImageRequest.fromUri(lowResImageUri))
            .setImageRequest(ImageRequest.fromUri(mainImageUri))
            .build()

        if (binding.image.controller == null || !controller.isSameImageRequest(binding.image.controller)) {
            binding.image.controller = controller
        }
    }

    private fun requestFullSizeImage() {
        if (!fullImageRequested) {
            fullImageRequested = true
            viewModel.loadSingleImage(nodeHandle, fullSize = true, highPriority = true)
        }
    }

    private fun launchVideoScreen(item: ImageItem) {
        val fileUri = item.imageResult?.getHighestResolutionAvailableUri()
        val nodeName = item.nodeItem?.node?.name
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            setDataAndType(fileUri, MimeTypeList.typeForName(nodeName).type)
            putExtra(INTENT_EXTRA_KEY_HANDLE, item.handle)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, nodeName)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_IMAGE_VIEWER)
            putExtra(INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
            putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, false)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(intent)
    }

    private fun isHighResolutionRestricted(): Boolean =
        requireContext().isMeteredConnection()
                && !preferences.getBoolean(SettingsConstants.KEY_MOBILE_DATA_HIGH_RESOLUTION, true)
}
