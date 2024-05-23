package mega.privacy.android.app.utils

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ProgressBar
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.icon.pack.R
import timber.log.Timber

object FrescoUtils {

    /**
     * Load GIF/WEBP to display the animation.
     * SimpleDraweeView handles with cache and resource release.
     *
     * @param gifImgDisplay            The SimpleDraweeView to display the GIF/WEBP.
     * @param pb                       Progress bar showing when loading.
     * @param shouldDisplayPlaceHolder If true, a placeholder should be shown while the animated image is loading.
     * @param placeholder                 Used as placeholder, before the GIF/WEBP is fully loaded.
     * @param uri                      The uri of GIF/WEBP. May be from url or local path.
     */
    fun loadGif(
        gifImgDisplay: SimpleDraweeView,
        pb: ProgressBar?,
        shouldDisplayPlaceHolder: Boolean,
        placeholder: Drawable?,
        uri: Uri?,
    ) {
        // Set placeholder and its scale type here rather than in xml.
        if (shouldDisplayPlaceHolder) {
            if (placeholder == null) {
                gifImgDisplay.hierarchy.setPlaceholderImage(
                    R.drawable.ic_image_medium_solid,
                    ScalingUtils.ScaleType.CENTER_INSIDE
                )
            } else {
                gifImgDisplay.hierarchy.setPlaceholderImage(
                    placeholder,
                    ScalingUtils.ScaleType.CENTER_INSIDE
                )
            }
        }
        val imageRequest = ImageRequest.fromUri(uri)
        val controller: DraweeController = Fresco.newDraweeControllerBuilder()
            .setImageRequest(imageRequest)
            .setAutoPlayAnimations(true)
            .setControllerListener(object : BaseControllerListener<ImageInfo?>() {
                override fun onFinalImageSet(
                    id: String,
                    imageInfo: ImageInfo?,
                    animatable: Animatable?,
                ) {
                    hideProgressBar(pb)
                }

                override fun onFailure(id: String, throwable: Throwable) {
                    hideProgressBar(pb)
                    Timber.w(throwable, "Load gif failed, error")
                }
            })
            .build()
        gifImgDisplay.controller = controller
    }

    /**
     * Load GIF/WEBP to display the animation.
     * SimpleDraweeView handles with cache and resource release.
     *
     * @param gifImgDisplay The SimpleDraweeView to display the GIF/WEBP.
     * @param uri           The uri of GIF/WEBP. May be from url or local path.
     */
    fun loadGif(gifImgDisplay: SimpleDraweeView, uri: Uri?) {
        loadGif(gifImgDisplay, null, false, null, uri)
    }

    private fun hideProgressBar(pb: ProgressBar?) {
        if (pb != null) {
            pb.visibility = View.GONE
        }
    }
}

/**
 * Helper method to load Uri into a SimpleDraweeView while resizing it to the View size
 */
fun SimpleDraweeView.setImageRequestFromUri(uri: Uri?) {
    if (uri == null) return

    setImageRequest(
        ImageRequestBuilder.newBuilderWithSource(uri)
            .setRotationOptions(RotationOptions.autoRotate())
            .setResizeOptions(ResizeOptions.forDimensions(width, height))
            .build()
    )
}
