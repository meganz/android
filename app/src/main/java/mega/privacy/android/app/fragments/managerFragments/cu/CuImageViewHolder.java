package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
import mega.privacy.android.app.databinding.ItemCameraUploadsImageBinding;

class CuImageViewHolder extends CuViewHolder {

    private final ItemCameraUploadsImageBinding mBinding;
    private final CuItemSizeConfig mItemSizeConfig;

    public CuImageViewHolder(ItemCameraUploadsImageBinding binding,
            CuItemSizeConfig itemSizeConfig) {
        super(binding.getRoot());
        mBinding = binding;
        mItemSizeConfig = itemSizeConfig;

        setViewSize(binding.getRoot(), binding.icSelected, binding.thumbnail, itemSizeConfig);
    }

    @Override protected void bind(CuNode node) {
        mBinding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);

        updateThumbnailDisplay(mBinding.thumbnail, node, mItemSizeConfig);
    }

    public ItemCameraUploadsImageBinding binding() {
        return mBinding;
    }
}
