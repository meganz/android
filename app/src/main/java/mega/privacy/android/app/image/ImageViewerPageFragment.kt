package mega.privacy.android.app.image

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.RetainingDataSourceSupplier
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.databinding.PageImageViewerBinding
import mega.privacy.android.app.image.ui.DoubleTapGestureListener
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import nz.mega.documentscanner.utils.IntentUtils.extraNotNull
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE

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

    private lateinit var binding: PageImageViewerBinding

    private val nodeHandle: Long by extraNotNull(INTENT_EXTRA_KEY_HANDLE, INVALID_HANDLE)
    private val viewModel by activityViewModels<ImageViewerViewModel>()
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
        require(nodeHandle != INVALID_HANDLE) { "Invalid image node handle" }

        setupView()
        setupObservers()
    }

    private fun setupView() {
        binding.image.apply {
            setZoomingEnabled(true)
            setIsLongpressEnabled(true)
            setAllowTouchInterceptionWhileZoomed(true)
            setTapListener(DoubleTapGestureListener(this))

            controller = Fresco.newDraweeControllerBuilder()
                .setOldController(controller)
                .setAutoPlayAnimations(true)
                .setDataSourceSupplier(retainingSupplier)
                .build()
        }
    }

    private fun setupObservers() {
        val isResumed = viewLifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED

        viewModel.loadSingleImage(nodeHandle, isResumed).observe(viewLifecycleOwner) { item ->
            binding.txtTitle.text = item.name

            retainingSupplier.replaceSupplier(
                Fresco.getImagePipeline().getDataSourceSupplier(
                    ImageRequest.fromUri(item.getAvailableUri()),
                    null,
                    ImageRequest.RequestLevel.FULL_FETCH
                )
            )
        }
    }
}
