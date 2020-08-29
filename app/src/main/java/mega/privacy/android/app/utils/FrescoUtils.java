package mega.privacy.android.app.utils;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;

import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class FrescoUtils {

    /**
     * Load GIF/WEBP to display the animation.
     * SimpleDraweeView handles with cache and resource release.
     *
     * @param gifImgDisplay The SimpleDraweeView to display the GIF/WEBP.
     * @param pb Progress bar showing when loading.
     * @param drawable Used as placeholder, before the GIF/WEBP is fully loaded.
     * @param uri The uri of GIF/WEBP. May be from url or local path.
     */
    public static void loadGif(SimpleDraweeView gifImgDisplay, ProgressBar pb, @Nullable Drawable drawable, Uri uri) {
        // Set placeholder and its scale type here rather than in xml.
        if (drawable == null) {
            gifImgDisplay.getHierarchy().setPlaceholderImage(R.drawable.ic_image_thumbnail, ScalingUtils.ScaleType.CENTER_INSIDE);
        } else {
            gifImgDisplay.getHierarchy().setPlaceholderImage(drawable, ScalingUtils.ScaleType.CENTER_INSIDE);
        }
        ImageRequest imageRequest = ImageRequest.fromUri(uri);
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setAutoPlayAnimations(true)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {

                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        pb.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        pb.setVisibility(View.GONE);
                        logWarning("Load gif failed, error: " + throwable.getMessage());
                    }
                })
                .build();
        gifImgDisplay.setController(controller);
    }
}
