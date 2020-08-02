package mega.privacy.android.app.fragments.managerFragments.cu;

import android.view.View;
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

    @Override protected void bind(CuNode node) {
        updateThumbnailDisplay(mBinding.thumbnail, node, mItemSizeConfig);

        mBinding.icSelected.setVisibility(node.isSelected() ? View.VISIBLE : View.GONE);
    }

    public ItemCameraUploadsImageBinding binding() {
        return mBinding;
    }
}
