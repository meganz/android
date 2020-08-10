package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.RequestManager;

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
}
