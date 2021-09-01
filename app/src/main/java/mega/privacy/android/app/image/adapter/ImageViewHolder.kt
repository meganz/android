package mega.privacy.android.app.image.adapter

import androidx.recyclerview.widget.RecyclerView
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.RetainingDataSourceSupplier
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import mega.privacy.android.app.databinding.PageImageViewerBinding
import mega.privacy.android.app.image.data.ImageItem
import mega.privacy.android.app.image.ui.DoubleTapGestureListener

class ImageViewHolder(
    private val binding: PageImageViewerBinding
) : RecyclerView.ViewHolder(binding.root) {

    private val retainingSupplier =
        RetainingDataSourceSupplier<CloseableReference<CloseableImage>>()

    init {
        binding.image.apply {
            setZoomingEnabled(true)
            setIsLongpressEnabled(true)
            setAllowTouchInterceptionWhileZoomed(true)
            setTapListener(DoubleTapGestureListener(this))

            controller = Fresco.newDraweeControllerBuilder()
                .setAutoPlayAnimations(true)
                .setDataSourceSupplier(retainingSupplier)
                .setOldController(binding.image.controller)
                .build()
        }
    }

    fun bind(item: ImageItem) {
        binding.txtTitle.text = item.name
//        binding.image.setImageURI(item.uri)

        retainingSupplier.replaceSupplier(
            Fresco.getImagePipeline().getDataSourceSupplier(
                ImageRequest.fromUri(item.uri),
                null,
                ImageRequest.RequestLevel.FULL_FETCH
            )
        )
    }
}
