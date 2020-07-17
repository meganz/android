package mega.privacy.android.app.adapters;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import mega.privacy.android.app.R;
import mega.privacy.android.app.fragments.managerFragments.cu.CuItemSizeConfig;
import mega.privacy.android.app.fragments.managerFragments.cu.CuNode;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
abstract class CuViewHolder extends RecyclerView.ViewHolder {

  public CuViewHolder(@NonNull View itemView) {
    super(itemView);
  }

  public void bind(int position, CuNode node, CameraUploadsAdapter.Listener listener) {
    if (handleClick() && node.getNode() != null) {
      itemView.setOnClickListener(v -> listener.onNodeClicked(position, node));
      itemView.setOnLongClickListener(v -> {
        listener.onNodeLongClicked(position, node);
        return true;
      });
    }
    bind(node);
  }

  protected abstract void bind(CuNode node);

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

  static void updateThumbnailDisplay(ImageView thumbnail, CuNode node,
      CuItemSizeConfig itemSizeConfig) {
    RequestBuilder<Drawable> requestBuilder;
    if (node.getThumbnail() != null) {
      requestBuilder = Glide.with(thumbnail).load(node.getThumbnail())
          .placeholder(R.drawable.ic_image_thumbnail);
    } else {
      requestBuilder = Glide.with(thumbnail).load(R.drawable.ic_image_thumbnail);
    }
    if (node.isSelected()) {
      requestBuilder = requestBuilder
          .transform(new CenterCrop(), new RoundedCorners(itemSizeConfig.getRoundCornerRadius()));
    } else {
      requestBuilder = requestBuilder.transform(new CenterCrop());
    }
    requestBuilder
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(thumbnail);

    int padding = node.isSelected() ? itemSizeConfig.getSelectedPadding() : 0;
    thumbnail.setPadding(padding, padding, padding, padding);
  }
}
