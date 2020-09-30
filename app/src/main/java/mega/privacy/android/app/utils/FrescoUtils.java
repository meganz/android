package mega.privacy.android.app.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequest;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;

import static mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter.updateViewDimensions;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class FrescoUtils {

    /**
     * Load GIF/WEBP to display the animation.
     * SimpleDraweeView handles with cache and resource release.
     *
     * @param gifImgDisplay            The SimpleDraweeView to display the GIF/WEBP.
     * @param pb                       Progress bar showing when loading.
     * @param shouldDisplayPlaceHolder If true, a placeholder should be shown while the animated image is loading.
     * @param drawable                 Used as placeholder, before the GIF/WEBP is fully loaded.
     * @param uri                      The uri of GIF/WEBP. May be from url or local path.
     */
    public static void loadGif(SimpleDraweeView gifImgDisplay, ProgressBar pb, boolean shouldDisplayPlaceHolder, @Nullable Drawable drawable, Uri uri) {
        // Set placeholder and its scale type here rather than in xml.
        if (shouldDisplayPlaceHolder) {
            if (drawable == null) {
                gifImgDisplay.getHierarchy().setPlaceholderImage(R.drawable.ic_image_thumbnail, ScalingUtils.ScaleType.CENTER_INSIDE);
            } else {
                gifImgDisplay.getHierarchy().setPlaceholderImage(drawable, ScalingUtils.ScaleType.CENTER_INSIDE);
            }
        }

        ImageRequest imageRequest = ImageRequest.fromUri(uri);
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setAutoPlayAnimations(true)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {

                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        if (pb != null) {
                            pb.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        if (pb != null) {
                            pb.setVisibility(View.GONE);
                        }
                        logWarning("Load gif failed, error: " + throwable.getMessage());
                    }
                })
                .build();
        gifImgDisplay.setController(controller);
    }

    /**
     * Load GIF/WEBP to display the animation.
     * SimpleDraweeView handles with cache and resource release.
     *
     * @param gifImgDisplay The SimpleDraweeView to display the GIF/WEBP.
     * @param pb            Progress bar showing when loading.
     * @param drawable      Used as placeholder, before the GIF/WEBP is fully loaded.
     * @param uri           The uri of GIF/WEBP. May be from url or local path.
     */
    public static void loadGif(SimpleDraweeView gifImgDisplay, ProgressBar pb, @Nullable Drawable drawable, Uri uri) {
        loadGif(gifImgDisplay, pb, true, drawable, uri);
    }

    /**
     * Load GIF/WEBP to display the animation.
     * SimpleDraweeView handles with cache and resource release.
     *
     * @param gifImgDisplay The SimpleDraweeView to display the GIF/WEBP.
     * @param uri           The uri of GIF/WEBP. May be from url or local path.
     */
    public static void loadGif(SimpleDraweeView gifImgDisplay, Uri uri) {
        loadGif(gifImgDisplay, null, false, null, uri);
    }

    /**
     * Load a local file into a ordinary ImageView.
     *
     * @param imageView An ordinary ImageView used to display the image.
     * @param pb Progress bar, should be dismissed after the image is displayed.
     * @param uri Uri of the local image file.
     */
    public static void loadImage(ImageView imageView, ProgressBar pb, Uri uri) {
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        ImageRequest imageRequest = ImageRequest.fromUri(uri);


        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);

        dataSource.subscribe(new BaseBitmapDataSubscriber() {

            @Override
            public void onNewResultImpl(@Nullable Bitmap bitmap) {
                if (bitmap != null && !bitmap.isRecycled()) {
                    // Work around: bitmap will be recylced by Fresco soon, create a copy then use the copy.
                    Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                    pb.setVisibility(View.GONE);
                    imageView.setImageBitmap(copy);
                }
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                // No cleanup required here.
            }
        }, UiThreadImmediateExecutorService.getInstance());
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
    public static void loadGifMessage(SimpleDraweeView gifImgDisplay, ProgressBar pb, RoundedImageView preview, RelativeLayout fileView, Uri uri) {
        if (gifImgDisplay == null) {
            logWarning("Unable to load GIF, view is null.");
            return;
        }

        gifImgDisplay.setVisibility(View.VISIBLE);

        if (pb != null) {
            pb.setVisibility(View.VISIBLE);
        }

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(ImageRequest.fromUri(uri))
                .setAutoPlayAnimations(true)
                .setControllerListener(new BaseControllerListener<ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
                        updateViewDimensions(gifImgDisplay, imageInfo.getWidth(), imageInfo.getHeight());

                        if (pb != null) {
                            pb.setVisibility(View.GONE);
                        }

                        if (fileView != null) {
                            fileView.setVisibility(View.GONE);
                        }

                        if (preview != null) {
                            preview.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        gifImgDisplay.setVisibility(View.GONE);

                        if (pb != null) {
                            pb.setVisibility(View.GONE);
                        }
                        logWarning("Load gif failed, error: " + throwable.getMessage());
                    }
                })
                .build();

        gifImgDisplay.setController(controller);
    }
}
