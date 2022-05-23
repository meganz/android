package mega.privacy.android.app.utils

import android.graphics.Bitmap
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.facebook.common.executors.UiThreadImmediateExecutorService
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.components.RoundedImageView
import mega.privacy.android.app.main.megachat.chatAdapters.MegaChatAdapter
import mega.privacy.android.app.utils.LogUtil.logWarning

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
        uri: Uri?
    ) {
        // Set placeholder and its scale type here rather than in xml.
        if (shouldDisplayPlaceHolder) {
            if (placeholder == null) {
                gifImgDisplay.hierarchy.setPlaceholderImage(
                    R.drawable.ic_image_thumbnail,
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
                    animatable: Animatable?
                ) {
                    hideProgressBar(pb)
                }

                override fun onFailure(id: String, throwable: Throwable) {
                    hideProgressBar(pb)
                    logWarning("Load gif failed, error: " + throwable.message)
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
     * @param pb            Progress bar showing when loading.
     * @param placeholder      Used as placeholder, before the GIF/WEBP is fully loaded.
     * @param uri           The uri of GIF/WEBP. May be from url or local path.
     */
    fun loadGif(
        gifImgDisplay: SimpleDraweeView,
        pb: ProgressBar?,
        placeholder: Drawable?,
        uri: Uri?
    ) {
        loadGif(gifImgDisplay, pb, true, placeholder, uri)
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

    /**
     * Load a local file into a ordinary ImageView.
     *
     * @param imageView An ordinary ImageView used to display the image.
     * @param pb Progress bar, should be dismissed after the image is displayed.
     * @param uri Uri of the local image file.
     */
    fun loadImage(imageView: ImageView, pb: ProgressBar?, uri: Uri?) {
        val imagePipeline = Fresco.getImagePipeline()
        val imageRequest = ImageRequest.fromUri(uri)
        imagePipeline.fetchDecodedImage(imageRequest, null)
            .subscribe(object : BaseBitmapDataSubscriber() {
                public override fun onNewResultImpl(bitmap: Bitmap?) {
                    if (bitmap != null && !bitmap.isRecycled) {
                        // Work around: bitmap will be recylced by Fresco soon, create a copy then use the copy.
                        val copy = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        hideProgressBar(pb)
                        imageView.setImageBitmap(copy)
                    }
                }

                override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                    // No cleanup required here.
                }
            }, UiThreadImmediateExecutorService.getInstance())
    }

    /**
     * Load GIF/WEBP in a chat message.
     * SimpleDraweeView handles with cache and resource release.
     *
     * @param gifImgDisplay The SimpleDraweeView to display the GIF/WEBP.
     * @param pb            Progress bar showing when loading.
     * @param preview       View where the file preview is shown.
     * @param fileView      View where the file inso is shown.
     * @param uri           The uri of GIF/WEBP. May be from url or local path.
     */
    @JvmStatic
    fun loadGifMessage(
        gifImgDisplay: SimpleDraweeView?,
        pb: ProgressBar?,
        preview: RoundedImageView?,
        fileView: RelativeLayout?,
        uri: Uri?
    ) {
        if (gifImgDisplay == null) {
            logWarning("Unable to load GIF, view is null.")
            return
        }
        if (gifImgDisplay.visibility != View.VISIBLE) {
            gifImgDisplay.visibility = View.VISIBLE
        }
        if (pb != null) {
            pb.visibility = View.VISIBLE
        }
        val controller: DraweeController = Fresco.newDraweeControllerBuilder()
            .setImageRequest(ImageRequest.fromUri(uri))
            .setAutoPlayAnimations(true)
            .setControllerListener(object : BaseControllerListener<ImageInfo?>() {
                override fun onFinalImageSet(
                    id: String,
                    imageInfo: ImageInfo?,
                    animatable: Animatable?
                ) {
                    MegaChatAdapter.updateViewDimensions(
                        gifImgDisplay,
                        imageInfo!!.width,
                        imageInfo.height
                    )
                    hideProgressBar(pb)
                    if (fileView != null && fileView.visibility != View.GONE) {
                        fileView.visibility = View.GONE
                    }
                    if (preview != null) {
                        preview.visibility = View.GONE
                    }
                }

                override fun onFailure(id: String, throwable: Throwable) {
                    logWarning("Load gif failed, error: " + throwable.message)
                }
            })
            .build()
        gifImgDisplay.controller = controller
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
