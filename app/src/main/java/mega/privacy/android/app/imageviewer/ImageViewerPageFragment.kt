package mega.privacy.android.app.imageviewer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.RetainingDataSourceSupplier
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
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

@AndroidEntryPoint
class ImageViewerPageFragment : Fragment() {

    companion object {
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
    private var fullSizeRequested = false
    private val nodeHandle: Long by extraNotNull(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
    private val retainingSupplier by lazy { RetainingDataSourceSupplier<CloseableReference<CloseableImage>>() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PageImageViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        require(nodeHandle != INVALID_HANDLE) { "Invalid node handle" }

        setupView()
        setupObservers()
    }

    private fun setupView() {
        binding.image.apply {
            setZoomingEnabled(true)
            setIsLongpressEnabled(true)
            setAllowTouchInterceptionWhileZoomed(false)
            setTapListener(
                MultiTapGestureListener(this,
                    onSingleTapCallback = {
                        viewModel.switchToolbar()
                    },
                    onZoomCallback = {
                        if (!fullSizeRequested && !isHighResolutionRestricted()) {
                            fullSizeRequested = true
                            viewModel.reloadCurrentImage(true)
                        }
                    }
                )
            )

            controller = Fresco.newDraweeControllerBuilder()
                .setOldController(controller)
                .setAutoPlayAnimations(true)
                .setDataSourceSupplier(retainingSupplier)
                .build()
        }
    }

    private fun setupObservers() {
        viewModel.getImage(nodeHandle).observe(viewLifecycleOwner, ::showImageItem)
        viewModel.loadSingleImage(nodeHandle, false).observe(viewLifecycleOwner) {
            binding.progress.hide()
        }
    }

    private fun showImageItem(item: ImageItem?) {
        if (item != null) {
            retainingSupplier.replaceSupplier(
                Fresco.getImagePipeline().getDataSourceSupplier(
                    ImageRequest.fromUri(item.getAvailableUri()),
                    null,
                    ImageRequest.RequestLevel.FULL_FETCH
                )
            )

            if (item.isVideo) {
                binding.image.setOnClickListener { launchVideoScreen(item) }
                binding.btnVideo.setOnClickListener { launchVideoScreen(item) }
            }
            binding.btnVideo.isVisible = item.isVideo
        } else {
            logError("ImageItem is null")
            Toast.makeText(requireContext(), R.string.error_fail_to_open_file_general, Toast.LENGTH_LONG).show()
            activity?.finish()
        }
    }

    private fun launchVideoScreen(item: ImageItem) {
        val fileUri = item.getAvailableUri()
        val intent = Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra(INTENT_EXTRA_KEY_HANDLE, item.handle)
            putExtra(INTENT_EXTRA_KEY_FILE_NAME, item.name)
            putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false)
            putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, INBOX_ADAPTER)
            putExtra(INTENT_EXTRA_KEY_POSITION, 0)
            putExtra(INTENT_EXTRA_KEY_PARENT_NODE_HANDLE, INVALID_HANDLE)
            putExtra(INTENT_EXTRA_KEY_ORDER_GET_CHILDREN, ORDER_DEFAULT_ASC)
            putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setDataAndType(fileUri, MimeTypeList.typeForName(item.name).type)
        }

        startActivity(intent)
    }

    private fun isHighResolutionRestricted(): Boolean =
        !requireContext().isMeteredConnection()
                || !preferences.getBoolean(SettingsConstants.KEY_MOBILE_DATA_HIGH_RESOLUTION, true)
}
