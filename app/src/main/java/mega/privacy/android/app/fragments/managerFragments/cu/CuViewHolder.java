package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;

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

    protected void updateThumbnailDisplay(ShapeableImageView imageView, CuNode node, RequestManager requestManager) {
        int shapeId = node.isSelected() ? R.style.GalleryImageShape_Selected : R.style.GalleryImageShape;
        imageView.setShapeAppearanceModel(
                ShapeAppearanceModel.builder(itemView.getContext(), shapeId, 0).build()
        );

        requestManager.load(node.getThumbnail())
                .placeholder(R.drawable.ic_image_thumbnail)
                .error(R.drawable.ic_image_thumbnail)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(imageView);
    }
}
