package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.shape.ShapeAppearanceModel;

import mega.privacy.android.app.R;
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding;

/**
 * Created by Piasy{github.com/Piasy} on 2020/7/17.
 */
class CuImageViewHolder extends CuViewHolder {

    private final ItemCameraUploadsImageBinding mBinding;
    private final CuItemSizeConfig mItemSizeConfig;

    public CuImageViewHolder(ItemCameraUploadsImageBinding binding,
            CuItemSizeConfig itemSizeConfig) {
        super(binding.getRoot());
        mBinding = binding;
        mItemSizeConfig = itemSizeConfig;

        setViewSize(binding.getRoot(), binding.icSelected, itemSizeConfig);
    }

    @Override protected void bind(CuNode node, RequestManager requestManager) {
        mBinding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);

        int shapeId = node.isSelected() ? R.style.GalleryImageShape_Selected : R.style.GalleryImageShape;
        mBinding.thumbnail.setShapeAppearanceModel(
                ShapeAppearanceModel.builder(itemView.getContext(), shapeId, 0).build()
        );

        requestManager.load(node.getThumbnail())
                .placeholder(R.drawable.ic_image_thumbnail)
                .error(R.drawable.ic_image_thumbnail)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(mBinding.thumbnail);
    }

    public ItemCameraUploadsImageBinding binding() {
        return mBinding;
    }
}
