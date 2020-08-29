package mega.privacy.android.app.utils;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;

import java.io.File;

import mega.privacy.android.app.components.TouchImageView;

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
        gifImgDisplay.getHierarchy().setPlaceholderImage(drawable);
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

    public static void loadImage(TouchImageView imageView, File file) {
//        ImageRequest imageRequest = ImageRequest.;
//        DraweeController controller = Fresco.newDraweeControllerBuilder()
//                .setImageRequest(imageRequest)
//                .build();
//        imageView.setController(controller);
        imageView.setImageURI(Uri.fromFile(file));
    }
}
