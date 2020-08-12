package mega.privacy.android.app.fragments.managerFragments.cu;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import mega.privacy.android.app.R;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
abstract class CuViewHolder extends RecyclerView.ViewHolder {

    public CuViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public void bind(int position, CuNode node, CameraUploadsAdapter.Listener listener, RequestManager requestManager) {
        if (handleClick() && node.getNode() != null) {
            itemView.setOnClickListener(v -> listener.onNodeClicked(position, node));
            itemView.setOnLongClickListener(v -> {
                listener.onNodeLongClicked(position, node);
                return true;
            });
        }

        bind(node, requestManager);
    }

    protected abstract void bind(CuNode node, RequestManager requestManager);

    protected boolean handleClick() {
        return true;
    }

    static void setViewSize(View grid, View icSelected, CuItemSizeConfig itemSizeConfig) {
        GridLayoutManager.LayoutParams params =
                (GridLayoutManager.LayoutParams) grid.getLayoutParams();
        params.width = itemSizeConfig.getGridSize();
        params.height = itemSizeConfig.getGridSize();
        params.topMargin = itemSizeConfig.getGridMargin();
        params.bottomMargin = itemSizeConfig.getGridMargin();
        params.setMarginStart(itemSizeConfig.getGridMargin());
        params.setMarginEnd(itemSizeConfig.getGridMargin());
        grid.setLayoutParams(params);

        FrameLayout.LayoutParams icSelectedParams =
                (FrameLayout.LayoutParams) icSelected.getLayoutParams();
        icSelectedParams.width = itemSizeConfig.getIcSelectedSize();
        icSelectedParams.height = itemSizeConfig.getIcSelectedSize();
        icSelectedParams.topMargin = itemSizeConfig.getIcSelectedMargin();
        icSelectedParams.setMarginStart(itemSizeConfig.getIcSelectedMargin());
        icSelected.setLayoutParams(icSelectedParams);
    }

    protected void updateThumbnailDisplay(ImageView imageView, CuNode node,
                                          CuItemSizeConfig itemSizeConfig, RequestManager requestManager) {
        RequestBuilder<Drawable> request = requestManager.load(node.getThumbnail())
                .error(R.drawable.ic_image_thumbnail)
                .transition(DrawableTransitionOptions.withCrossFade())
                .addListener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        int padding;
                        if (node.isSelected()) {
                            padding = itemSizeConfig.getSelectedPadding();
                            imageView.setBackgroundResource(R.drawable.background_item_grid_selected);
                        } else {
                            padding = 0;
                            imageView.setBackground(null);
                        }
                        imageView.setPadding(padding, padding, padding, padding);
                        return false;
                    }
                });

        if (node.isSelected()) {
            request.transform(new RoundedCorners(itemSizeConfig.getRoundCornerRadius()));
        } else {
            request.placeholder(R.drawable.ic_image_thumbnail);
        }

        request.into(imageView);
    }
}
