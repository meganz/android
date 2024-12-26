package mega.privacy.android.app.utils

import android.net.Uri
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.request.ImageRequestBuilder

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
